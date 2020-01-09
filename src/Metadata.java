import java.io.Serializable;

class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;
    int [] downloadStatus;
    int downloadSize;
// TODO: Use this to store percent count / number of chunks downloaded
//    int downloadCount;
    Metadata(int size, int chunkCount){
        downloadStatus = new int[chunkCount];
        downloadSize = size;
    }

}
