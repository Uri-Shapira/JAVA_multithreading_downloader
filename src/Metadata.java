import java.io.Serializable;

class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;
    boolean [] chunksDownloaded;
    int downloadSize;
    int bytesDownloadedAlready;
    int chunksDownloadedAlready;

    Metadata(int size, int chunkCount) {
        chunksDownloaded = new boolean[chunkCount];
        downloadSize = size;
        bytesDownloadedAlready = 0;
        chunksDownloadedAlready = 0;
    }

    int getPercentDownloaded(){
        double percent = (double)bytesDownloadedAlready / (double)downloadSize * 100;
        return (int)percent;
    }

}
