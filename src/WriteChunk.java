class WriteChunk {

    int m_metadataIndex;
    long m_writePosition;
    byte[] m_chunkToWrite;
    int m_chunkSize;
    boolean m_downloadFinished = false;

    WriteChunk(int i_metadataIndex, long i_writePosition, byte[] i_chunkToWrite, int i_chunkSize){
        m_metadataIndex = i_metadataIndex;
        m_writePosition = i_writePosition;
        m_chunkToWrite = i_chunkToWrite.clone();
        m_chunkSize = i_chunkSize;
    }

    WriteChunk(boolean i_downloadFinished){
        m_downloadFinished = i_downloadFinished;
    }

}
