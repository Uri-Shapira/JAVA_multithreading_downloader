import java.io.File;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

class DownloadManager {

    private final long m_chunkSize = 4096;
    private Metadata m_metadata;
    private String m_metadataFile;
    private int m_maximumConcurrentConnections;
    private ArrayList<Thread> m_concurrentConnections;
    private ArrayList<ArrayList<DownloadBatch>> m_batchesToDownload;
    private String m_downloadURL;
    private String m_fileName;

    DownloadManager(String downloadURL, int maximumConcurrentConnections){
        m_downloadURL = downloadURL;
        m_maximumConcurrentConnections = maximumConcurrentConnections;
        m_concurrentConnections = new ArrayList<>();
    }

    //TODO - add logic for calculating maximum concurrent in this case
    DownloadManager(String downloadURL){
        m_downloadURL = downloadURL;
    }

    private void initializeFiles() throws ClassNotFoundException{
        try{
            m_fileName = m_downloadURL.substring(m_downloadURL.lastIndexOf('/') +1);
            m_metadataFile = m_downloadURL.substring(m_downloadURL.lastIndexOf('/') +1, m_downloadURL.lastIndexOf('.')) + "_metadata";
            File newFile = new File(m_fileName);
            File metadataFile = new File(m_metadataFile);
            if(!metadataFile.exists()){
                newFile.createNewFile();
                metadataFile.createNewFile();
                getDownloadSize();
            }
            else{
                ObjectInputStream metadataStream = new ObjectInputStream(new FileInputStream(new File(m_metadataFile)));
                m_metadata = (Metadata) metadataStream.readObject();
            }
            getDownloadBatches();
            int index = 1;
//            for(ArrayList<DownloadBatch> _list : m_batchesToDownload){
//                System.out.println("-----------------------------------");
//                System.out.println("LIST NUMBER " + index);
//                System.out.println("-----------------------------------");
//                for(DownloadBatch batch : _list){
//                    System.out.println("START " + batch.m_startingPoint + " END " + batch.m_endingPoint);
//                }
//                index++;
//            }
        }
        catch (IOException e){
            System.err.println("Download Failed. Failed To Create File.");
        }
    }

    private void getDownloadSize() throws MalformedURLException {
        URL url = new URL(m_downloadURL);
        HttpURLConnection connection = null;
        try{
            connection = (HttpURLConnection) url.openConnection();
            long downloadSize = connection.getContentLengthLong();
            int numberOfChunks = (int)downloadSize / (int)m_chunkSize;
            if(downloadSize % m_chunkSize != 0){
                numberOfChunks += 1;
            }
            m_metadata = new Metadata((int)downloadSize,numberOfChunks);
//            System.out.println("DOWNLOAD SIZE " + m_metadata.downloadSize);
//            System.out.println("number of chunks " + m_metadata.batchesDownloaded.length);

        }
        catch (IOException e){
            System.err.println(e.getMessage());
        }
        finally {
            if(connection != null){
                connection.disconnect();
            }
        }
    }

    private void getDownloadBatches(){
        ArrayList<DownloadBatch> initialBatches = new ArrayList<>();
        long startPointer = 0;
        long endPointer = 0;
        while(endPointer < m_metadata.batchesDownloaded.length){
            while (m_metadata.batchesDownloaded[(int)startPointer]){
                startPointer += 1;
                endPointer += 1;
            }
            while(!m_metadata.batchesDownloaded[(int)endPointer]){
                if(endPointer == m_metadata.batchesDownloaded.length - 1){
                    endPointer +=1;
                    break;
                }
                endPointer += 1;
            }
            initialBatches.add(new DownloadBatch(startPointer, endPointer - 1));
            endPointer += 1;
            startPointer = endPointer;
        }
        initializeBatchListForThreads(initialBatches);
    }

    private void initializeBatchListForThreads(ArrayList<DownloadBatch> batchesList){
        m_batchesToDownload = new ArrayList<>();
        int chunksPerThread = m_metadata.batchesDownloaded.length / m_maximumConcurrentConnections;
        int currentChunkCount = 0;
        ArrayList<DownloadBatch> currentList = new ArrayList<>();
        while(!batchesList.isEmpty()){
            if(m_batchesToDownload.size() == m_maximumConcurrentConnections - 1){
                ArrayList<DownloadBatch> listToAdd = (ArrayList)batchesList.clone();
                m_batchesToDownload.add(listToAdd);
                batchesList.clear();
            }
            else{
                DownloadBatch batch = batchesList.get(0);
                if(batch.m_size + currentChunkCount <= chunksPerThread){
                    currentList.add(batch);
                    batchesList.remove(batch);
                    if(batch.m_size + currentChunkCount == chunksPerThread){
                        ArrayList<DownloadBatch> listToAdd = (ArrayList)currentList.clone();
                        m_batchesToDownload.add(listToAdd);
                        currentList.clear();
                        currentChunkCount = 0;
                    }
                    else{
                        currentChunkCount += batch.m_size;
                    }
                }
                else{
                    long portionToAdd = chunksPerThread - currentChunkCount;
                    DownloadBatch newBatch = new DownloadBatch(batch.m_startingPoint,batch.m_startingPoint + portionToAdd);
                    currentList.add(newBatch);
                    DownloadBatch addToList = new DownloadBatch(batch.m_startingPoint + portionToAdd + 1, batch.m_endingPoint);
                    batchesList.remove(batch);
                    batchesList.add(addToList);
                    ArrayList<DownloadBatch> listToAdd = (ArrayList)currentList.clone();
                    m_batchesToDownload.add(listToAdd);
                    currentList.clear();
                    currentChunkCount = 0;
                }
            }
        }
    }

    void downloadFile() throws InterruptedException, ClassNotFoundException{
        initializeFiles();
        System.out.println("=====================================================================================");
        System.out.println("Downloading file " + m_fileName + " using " + m_maximumConcurrentConnections + " concurrent connections");
        System.out.println("=====================================================================================");
        System.out.println("TOTAL FILE SIZE: " + m_metadata.downloadSize);
        if (m_metadata.downloadSize == -1) {
            System.err.println("Download Failed");
        }
        else{
            for(ArrayList<DownloadBatch> threadBatches : m_batchesToDownload){
                Thread thread = new Thread(new Downloader(m_downloadURL, m_fileName, threadBatches, m_metadata, m_metadataFile));
                m_concurrentConnections.add(thread);
                thread.start();
            }
            int percentage;
            while(!downloadFinished()){
                if((percentage = m_metadata.getPercentDownloaded()) > 0){
                    System.out.println("Downloaded " + percentage + "%");
                    Thread.sleep(500);
                }
            }
            System.out.println("Download " + getDownloadFinishState());
        }
    }

    private boolean downloadFinished(){
        for(Thread runningThread : m_concurrentConnections){
            if(runningThread.isAlive()){
                return false;
            }
        }
        return true;
    }

    private String getDownloadFinishState(){
        System.out.println(m_metadata.bytesDownloadedAlready);
        if(m_metadata.getPercentDownloaded() == 100){
            File file = new File(m_metadataFile);
            file.delete();
            return "Succeeded";
        }
        return "Failed";
    }
}
