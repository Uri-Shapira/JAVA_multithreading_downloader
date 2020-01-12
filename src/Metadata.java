import java.io.Serializable;

class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;
    boolean [] batchesDownloaded;
    int downloadSize;
    int bytesDownloadedAlready;

    Metadata(int size, int chunkCount) {
        batchesDownloaded = new boolean[chunkCount];
        downloadSize = size;
        bytesDownloadedAlready = 0;
    }

    int getPercentDownloaded(){
        double percent =  (double)bytesDownloadedAlready / (double)downloadSize * 100;
        return (int)percent;
    }

}
