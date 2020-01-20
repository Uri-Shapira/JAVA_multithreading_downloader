import java.io.File;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class DownloadManager {

    private int m_chunkSize;
    private Metadata m_metadata;
    private String m_metadataFile;
    private int m_maximumConcurrentConnections = 0;
    private ArrayList<Thread> m_concurrentConnections;
    private ArrayList<ArrayList<ReadBatch>> m_batchesToDownload;
    private ArrayList<String> m_downloadURLs;
    private String m_fileName;

    DownloadManager(ArrayList<String> i_downloadURLs, int i_maximumConcurrentConnections, int i_chunkSize){
        m_downloadURLs = i_downloadURLs;
        m_maximumConcurrentConnections = i_maximumConcurrentConnections;
        m_concurrentConnections = new ArrayList<>();
        m_chunkSize = i_chunkSize;
    }

    DownloadManager(ArrayList<String> i_downloadURLs, int i_chunkSize){
        m_downloadURLs = i_downloadURLs;
        m_concurrentConnections = new ArrayList<>();
        m_chunkSize = i_chunkSize;
    }

    //TODO: FIX THIS!!!!!!!!!!!!!!!!!!!1
    private void setMaximumConnectionsByFileSize(){
        long ConnectionCountUpperBound = m_metadata.downloadSize / (m_chunkSize);
        if(m_maximumConcurrentConnections == 0 || m_maximumConcurrentConnections > ConnectionCountUpperBound){
            m_maximumConcurrentConnections = (int)ConnectionCountUpperBound;
        }
    }

    private String getRandomURL(){
        if(m_downloadURLs.size() == 1){
            return m_downloadURLs.get(0);
        }
        int random = (int)(Math.random() * (m_downloadURLs.size() - 1));
        return m_downloadURLs.get(random);
    }

    private void initializeFiles() throws ClassNotFoundException{
        try{
            String url = m_downloadURLs.get(0);
            m_fileName = url.substring(url.lastIndexOf('/') +1);
            m_metadataFile = url.substring(url.lastIndexOf('/') +1, url.lastIndexOf('.')) + "_metadata";
            File downloadTarget = new File(m_fileName);
            File metadataFile = new File(m_metadataFile);
            if(!metadataFile.exists()){
                boolean fileCreated = downloadTarget.createNewFile();
                boolean metadataCreated = metadataFile.createNewFile();
                if((!fileCreated && !downloadTarget.exists()) || !metadataCreated){
                    System.err.println("Failed to create files. Download failed.");
                    System.exit(1);
                }
                getDownloadSize();
            }
            else{
                ObjectInputStream metadataStream = new ObjectInputStream(new FileInputStream(new File(m_metadataFile)));
                m_metadata = (Metadata) metadataStream.readObject();
            }
            setMaximumConnectionsByFileSize();
            getDownloadBatches();
//            int index = 1;
//            for(ArrayList<ReadBatch> _list : m_batchesToDownload){
//                System.out.println("-----------------------------------");
//                System.out.println("LIST NUMBER " + index);
//                System.out.println("-----------------------------------");
//                for(ReadBatch batch : _list){
//                    System.out.println("START " + batch.m_startingPoint + " END " + batch.m_endingPoint);
//                }
//                index++;
//            }
        }
        catch (IOException e){
            System.err.println("Download Failed. Failed To Create File.");
            System.exit(1);
        }
    }
    private void getDownloadSize() throws MalformedURLException {
        URL url = new URL(m_downloadURLs.get(0));
        HttpURLConnection connection = null;
        try{
            connection = (HttpURLConnection) url.openConnection();
            long downloadSize = connection.getContentLengthLong();
            if (downloadSize == -1) {
                System.err.println("URL Invalid or Connection Failed, File Size Appears As -1. Download Failed!");
                File metadata = new File(m_metadataFile);
                File file = new File(m_fileName);
                boolean metadataDeleted = metadata.delete();
                boolean fileDeleted = file.delete();
                if(!metadataDeleted || !fileDeleted){
                    System.err.println("Failed to delete metadata file.");
                }
                System.exit(1);
            }
            int numberOfChunks = (int)downloadSize / m_chunkSize;
            if(downloadSize % m_chunkSize != 0){
                numberOfChunks += 1;
            }
            m_metadata = new Metadata((int)downloadSize,numberOfChunks);
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
        ArrayList<ReadBatch> initialBatches = new ArrayList<>();
        long startPointer = 0;
        long endPointer = 0;
        while(endPointer < m_metadata.chunksDownloaded.length){
            while (m_metadata.chunksDownloaded[(int)startPointer] && endPointer <= m_metadata.chunksDownloaded.length - 1){
                startPointer += 1;
                endPointer += 1;
                if(endPointer == m_metadata.chunksDownloaded.length){
                    break;
                }
            }
            if(endPointer < m_metadata.chunksDownloaded.length - 1){
                while(!m_metadata.chunksDownloaded[(int)endPointer] && endPointer <= m_metadata.chunksDownloaded.length - 1){
                    endPointer += 1;
                    if(endPointer == m_metadata.chunksDownloaded.length - 1){
                        break;
                    }
                }
            }
            if(startPointer < m_metadata.chunksDownloaded.length && endPointer < m_metadata.chunksDownloaded.length){
                if(!m_metadata.chunksDownloaded[(int)startPointer]){
                    initialBatches.add(new ReadBatch(startPointer, endPointer));
                    endPointer += 1;
                    startPointer = endPointer;
                }
                if(endPointer == m_metadata.chunksDownloaded.length - 1){
                    break;
                }
            }
        }
        initializeBatchListForThreads(initialBatches);
    }

    private void initializeBatchListForThreads(ArrayList<ReadBatch> i_batchesList){
        m_batchesToDownload = new ArrayList<>();
        int totalChunks = m_metadata.chunksDownloaded.length - m_metadata.chunksDownloadedAlready;
        int chunksPerThread = totalChunks / m_maximumConcurrentConnections;
        int currentChunkCount = 0;
        ArrayList<ReadBatch> currentList = new ArrayList<>();
        while(!i_batchesList.isEmpty()){
            if(m_batchesToDownload.size() == m_maximumConcurrentConnections - 1){
                ArrayList<ReadBatch> listToAdd = new ArrayList<>(i_batchesList);
                m_batchesToDownload.add(listToAdd);
                i_batchesList.clear();
            }
            else{
                ReadBatch batch = i_batchesList.get(0);
                if(batch.m_size + currentChunkCount <= chunksPerThread){
                    currentList.add(batch);
                    i_batchesList.remove(batch);
                    if(batch.m_size + currentChunkCount == chunksPerThread){
                        ArrayList<ReadBatch> listToAdd = new ArrayList<>(currentList);
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
                    ReadBatch newBatch = new ReadBatch(batch.m_startingPoint,batch.m_startingPoint + portionToAdd);
                    currentList.add(newBatch);
                    ReadBatch addToList = new ReadBatch(batch.m_startingPoint + portionToAdd + 1, batch.m_endingPoint);
                    i_batchesList.remove(batch);
                    i_batchesList.add(addToList);
                    ArrayList<ReadBatch> listToAdd = new ArrayList<>(currentList);
                    m_batchesToDownload.add(listToAdd);
                    currentList.clear();
                    currentChunkCount = 0;
                }
            }
        }
    }

    void downloadFile() throws ClassNotFoundException{
        System.out.println("Initializing Download...");
        initializeFiles();
        System.out.println("Downloading using " + m_maximumConcurrentConnections + " connections...");
        BlockingQueue<WriteChunk> writingQueue = new ArrayBlockingQueue<>(m_metadata.chunksDownloaded.length);
        Thread writerThread = new Thread(new Writer(m_metadata, m_fileName, m_metadataFile, writingQueue));
        writerThread.start();
        for(ArrayList<ReadBatch> threadBatches : m_batchesToDownload){
            Thread thread = new Thread(new Downloader(getRandomURL(), threadBatches, m_metadata,
                    m_chunkSize, writingQueue));
            m_concurrentConnections.add(thread);
            thread.start();
        }
        int percentage;
        // Use set to print only "unique" percentages, or increments where the user will actually see progress
        Set<Integer> downloadProgress = new HashSet<>();
        while(!downloadFinished()){
            percentage = m_metadata.getPercentDownloaded();
            if(!downloadProgress.contains(percentage) && percentage > 0){
                downloadProgress.add(percentage);
                System.out.println("Downloaded " + percentage + "%");
            }
        }
        // DownloadFinisher is a dummy chunk used to stop the writer thread once all reader threads have finished
        WriteChunk downloadFinisher = new WriteChunk(true);
        writingQueue.add(downloadFinisher);
        while(writerThread.isAlive()){
            percentage = m_metadata.getPercentDownloaded();
            if(!downloadProgress.contains(percentage) && percentage > 0){
                downloadProgress.add(percentage);
                System.out.println("Downloaded " + percentage + "%");
            }
        }
        System.out.println("Download " + getDownloadFinishState());
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
        for(boolean downloaded : m_metadata.chunksDownloaded){
            if(!downloaded){
                return "Failed";
            }
        }
        File file = new File(m_metadataFile);
        boolean metadataDeleted = file.delete();
        if(!metadataDeleted){
            System.err.println("Failed to delete metadata file.");
        }
        return "Succeeded";
    }
}
