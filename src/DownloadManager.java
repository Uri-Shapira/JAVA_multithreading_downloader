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
    private String m_downloadURL;
    private String m_fileName;
    // TODO - understand where this comes in (probably needs to be synchronized)
    private double m_bytesDownloaded;

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
        }
        catch (IOException e){
            System.err.println("Download Failed");
        }
    }

    private void getDownloadSize() throws MalformedURLException {
        URL url = new URL(m_downloadURL);
        try{
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            long downloadSize = connection.getContentLengthLong();
//            long threadSegment = m_downloadSize / m_maximumConcurrentConnections;
            int numberOfChunks = (int)downloadSize / (int)m_chunkSize;
//            if(downloadSegment % m_maximumConcurrentConnections != 0){
//                numberOfChunks += 1;
//            }
            m_metadata = new Metadata((int)downloadSize,numberOfChunks);
            System.out.println("DOWNLOAD SIZE " + m_metadata.downloadSize);
            System.out.println("number of chunks " + m_metadata.downloadStatus.length);
            connection.disconnect();
        }
        catch (IOException e){
            System.err.println(e.getMessage());
        }
    }

    void downloadFile() throws IOException, InterruptedException, ClassNotFoundException{
        initializeFiles();
        getDownloadSize();
        System.out.println("=====================================================================================");
        System.out.println("Downloading file " + m_fileName + " using " + m_maximumConcurrentConnections + " concurrent connections");
        System.out.println("=====================================================================================");
        System.out.println("TOTAL FILE SIZE: " + m_metadata.downloadSize);
        if (m_metadata.downloadSize == -1) {
            System.err.println("Download Failed");
        }
        else{
            long threadSegment = m_metadata.downloadSize / m_maximumConcurrentConnections;
            System.out.println("download Segment " + threadSegment);
            for(int i = 0; i < m_maximumConcurrentConnections; i++){
                long start = i * threadSegment;
                long end = (i+1) * threadSegment;
                if(i == m_maximumConcurrentConnections - 1){
                    end = m_metadata.downloadSize;
                }
                Thread thread = new Thread(new Downloader(m_downloadURL, m_fileName, start, end , m_metadata, m_metadataFile));
                m_concurrentConnections.add(thread);
                System.out.println("THREAD " + thread.getId() + " is downloading from " + start + " until " + end);
                thread.start();
            }
//            for(long i = 0; i < m_downloadSize; i += threadSegment){
//                long end = (i + threadSegment) < m_downloadSize ? i + downloadSegment : m_downloadSize;
//                Thread thread = new Thread(new Downloader(m_downloadURL, m_fileName, i, end , m_metadata, m_metadataFile));
//                m_concurrentConnections.add(thread);
//                System.out.println("THREAD " + thread.getId() + " is downloading from " + i + " until " + end);
//                thread.start();
//            }
            String status;
            while((status = getDownloadStatus()).equals("Download in progress...")){
                System.out.println(status);
                Thread.sleep(1000);
            }
            System.out.println(status + getDownloadFinishState());
        }
    }

    private String getDownloadStatus(){
        for(Thread runningThread : m_concurrentConnections){
            if(runningThread.isAlive()){
                return "Download in progress...";
            }
        }
        return "Download Finished With Status ";
    }

    private String getDownloadFinishState() throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("TEST_FILE4")));
        for(int i = 0; i < m_metadata.downloadStatus.length; i++){
            long current = i * 4096;
            writer.write("chunk " + current + " downloaded: " + m_metadata.downloadStatus[i] + "\n");
            if(m_metadata.downloadStatus[i] == 0){
                System.out.println(i + " is the one that failed");
                return "FAILURE";
            }
        }
        writer.flush();
        writer.close();
        return "SUCCESS";
    }
}
