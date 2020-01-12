import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Downloader implements Runnable{

    private final int m_chunk = 4096;
    private ArrayList<DownloadBatch> m_batchesToDownload;
    private String m_downloadURL;
    private String m_metadataFile;
    private Metadata m_metadata;
    private String m_downloadedFile;
    private long m_currentPosition;
    public Exception threadError = null;

    Downloader(String fileToDownload, String savedFileName, ArrayList<DownloadBatch> batchesToDownload, Metadata metadata, String metadataFile){
        m_downloadURL = fileToDownload;
        m_downloadedFile = savedFileName;
        m_metadataFile = metadataFile;
        m_metadata = metadata;
        m_batchesToDownload = batchesToDownload;
    }

    public void run() {
        HttpURLConnection HTTPUrlConnection = null;
        BufferedInputStream bufferedInputStream = null;
        RandomAccessFile writeFile = null;
        ObjectOutputStream metadataWriter = null;
        try{
            URL url = new URL(m_downloadURL);
            metadataWriter = new ObjectOutputStream(new FileOutputStream(new File(m_metadataFile)));
            for(DownloadBatch batch : m_batchesToDownload){
                HTTPUrlConnection = (HttpURLConnection) url.openConnection();
                m_currentPosition = batch.m_startingPoint;
                long startingPoint = batch.m_startingPoint * m_chunk;
                if(batch.m_startingPoint == 0){
                    startingPoint = 0;
                }
                long endPoint = (batch.m_endingPoint + 1) * m_chunk - 1;
                if(batch.m_endingPoint == m_metadata.chunksDownloaded.length - 1){
                    endPoint = m_metadata.downloadSize;
                }
                System.out.println("[" + Thread.currentThread().getId() + "] Started Downloading Range ("
                        + startingPoint + "-" + endPoint +") From:\n" + m_downloadURL);
                HTTPUrlConnection.setRequestProperty("Range", "bytes=" + startingPoint + "-" + endPoint);
                bufferedInputStream = new BufferedInputStream(HTTPUrlConnection.getInputStream());
                writeFile = new RandomAccessFile(m_downloadedFile, "rw");
                writeFile.seek(startingPoint);
                int read;
                byte[] bufferedData = new byte[m_chunk];
                while(m_currentPosition <= batch.m_endingPoint){
                    read = bufferedInputStream.read(bufferedData,0, m_chunk);
//                    if(read < 4096){
//                        System.out.println("[" +Thread.currentThread().getId() + "] index: " + m_currentPosition + " read amount: " + read + " end index " + batch.m_endingPoint + " end bytes " + endPoint);
//                    }
                    if(read == -1){
                        break;
                    }
                    else{
                        writeFile.write(bufferedData,0,read);
                        m_metadata.chunksDownloaded[(int)m_currentPosition] = true;
                        m_metadata.chunksDownloadedAlready += 1;
                        m_currentPosition += 1;
                        m_metadata.bytesDownloadedAlready += read;
                    }
                }
                HTTPUrlConnection.disconnect();
            }
            metadataWriter.writeObject(m_metadata);
            System.out.println("[" + Thread.currentThread().getId() + "] Finished downloading.");
        }
        catch (IOException e){
            threadError = e;
            System.err.println(e.getMessage());
        }
        finally{
            if (HTTPUrlConnection != null){
                HTTPUrlConnection.disconnect();
            }
            if (bufferedInputStream != null){
                try{
                    bufferedInputStream.close();
                }
                catch (IOException e){
                    System.err.println("Error closing stream");
                }
            }
            if (writeFile != null){
                try{
                    writeFile.close();
                }
                catch (IOException e){
                    System.err.println("Error closing stream");
                }

            }
            if (metadataWriter != null){
                try {
                    metadataWriter.close();
                }
                catch (IOException e){
                    System.err.println("Error closing stream");
                }
            }

        }
    }
}
