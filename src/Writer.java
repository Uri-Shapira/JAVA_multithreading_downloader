import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.nio.file.*;

class Writer implements Runnable {

    private Metadata m_metadata;
    private BlockingQueue<WriteChunk> m_writingQueue;
    private String m_writeFile;
    private String m_metadataFile;

    Writer(Metadata i_metadata, String i_downloadedFile, String i_metadataFile, BlockingQueue<WriteChunk> i_writeQueue){
        m_metadata = i_metadata;
        m_writingQueue = i_writeQueue;
        m_metadataFile = i_metadataFile;
        m_writeFile = i_downloadedFile;
    }

    public void run(){
        ObjectOutputStream metadataWriter = null;
        try(RandomAccessFile downloadedFile = new RandomAccessFile(m_writeFile, "rw")){
            while(true){
                if(!m_writingQueue.isEmpty()){
                    WriteChunk chunkToWrite = m_writingQueue.peek();
                    m_writingQueue.remove();
                    // m_downloadFinished will be true only when the dummy downloadFinisher chunk exits the queue
                    if(chunkToWrite.m_downloadFinished){
                        break;
                    }
                    try{
                        downloadedFile.seek(chunkToWrite.m_writePosition);
                        downloadedFile.write(chunkToWrite.m_chunkToWrite,0,chunkToWrite.m_chunkSize);
                        m_metadata.chunksDownloaded[chunkToWrite.m_metadataIndex] = true;
                        m_metadata.chunksDownloadedAlready += 1;
                        m_metadata.bytesDownloadedAlready += chunkToWrite.m_chunkSize;
                        metadataWriter = new ObjectOutputStream(new FileOutputStream(new File(m_metadataFile)));
                        metadataWriter.writeObject(m_metadata);
                        metadataWriter.close();

                    }
                    catch (IOException e){
                        System.err.println("Unable to write to output file. Download failed!");
                        System.exit(1);
                    }
                    finally {
                        if(metadataWriter != null){
                            try{
                                metadataWriter.close();
                            }
                            catch(IOException e){
                                System.err.println("Failed to close metadata file");
                                System.exit(1);
                            }
                        }

                    }
                }
            }
        }
        catch(IOException e){
            System.err.println("Failed to open random access file.");
            System.exit(1);
        }
    }
}