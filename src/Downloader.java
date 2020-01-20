import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class Downloader implements Runnable{

    private int m_chunk;
    private ArrayList<ReadBatch> m_batchesToRead;
    private String m_downloadURL;
    private Metadata m_metadata;
    private BlockingQueue<WriteChunk> m_writingQueue;

    Downloader(String i_fileToDownload, ArrayList<ReadBatch> i_batchesToDownload,
               Metadata i_metadata, int i_chunkSize, BlockingQueue<WriteChunk> i_writingQueue){
        m_downloadURL = i_fileToDownload;
        m_metadata = i_metadata;
        m_batchesToRead = i_batchesToDownload;
        m_chunk = i_chunkSize;
        m_writingQueue = i_writingQueue;
    }


    public void run() {
        HttpURLConnection HTTPUrlConnection = null;
        BufferedInputStream bufferedInputStream = null;
        try{
            URL url = new URL(m_downloadURL);
            for(ReadBatch batch : m_batchesToRead){
                HTTPUrlConnection = (HttpURLConnection) url.openConnection();
                long currentIndex = batch.m_startingPoint;
                long startingPoint = batch.m_startingPoint * m_chunk;
                if(batch.m_startingPoint == 0){
                    startingPoint = 0;
                }
                long endPoint = (batch.m_endingPoint + 1) * m_chunk - 1;
                if(batch.m_endingPoint == m_metadata.chunksDownloaded.length - 1){
                    endPoint = m_metadata.downloadSize;
                }
                System.out.println("[" + Thread.currentThread().getId() + "] Starting to Download Range ("
                        + startingPoint + "-" + endPoint +") From:\n" + m_downloadURL);
                HTTPUrlConnection.setRequestMethod("GET");
                HTTPUrlConnection.setRequestProperty("Range", "bytes=" + startingPoint + "-" + endPoint);
                bufferedInputStream = new BufferedInputStream(HTTPUrlConnection.getInputStream());
                int read;
                byte[] bufferedData = new byte[m_chunk];
                long currentPoint = startingPoint;
                    while(currentPoint < endPoint){
//                    if(currentIndex == m_metadata.chunksDownloaded.length - 1){
//                        int toRead = (int)(endPoint - currentPoint);
//                        read = bufferedInputStream.read(bufferedData,0, toRead);
//                    }
//                    else{
                        read = bufferedInputStream.readNBytes(bufferedData,0, m_chunk);
//                    }
                    if(read == -1){
                        break;
                    }
                    WriteChunk writeChunk = new WriteChunk((int)currentIndex, currentPoint, bufferedData, read);
                    try{
                        m_writingQueue.put(writeChunk);
                    }
                    catch(InterruptedException e) {
                        System.err.println("[" + Thread.currentThread().getId() + "] Failed to add chunk to writing queue ");
                    }
                    currentPoint += read;
                    currentIndex += 1;
                }
                HTTPUrlConnection.disconnect();
            }

            System.out.println("[" + Thread.currentThread().getId() + "] Finished Downloading.");
        }
        catch (IOException e){
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

        }
    }
}
