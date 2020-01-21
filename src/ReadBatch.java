class ReadBatch {

    long m_startingPoint;
    long m_endingPoint;
    long m_size;

    ReadBatch(long i_start, long i_end){
        m_endingPoint = i_end;
        m_startingPoint = i_start;
        m_size = m_endingPoint - m_startingPoint + 1;
    }

}