class DownloadBatch {
    long m_startingPoint;
    long m_endingPoint;
    long m_size;

    DownloadBatch(long start, long end){
        m_endingPoint = end;
        m_startingPoint = start;
        m_size = m_endingPoint - m_startingPoint + 1;
    }

}
