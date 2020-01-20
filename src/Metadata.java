import java.io.Serializable;

class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;
    boolean [] chunksDownloaded;
    int downloadSize;
    int bytesDownloadedAlready;
    int chunksDownloadedAlready;

    Metadata(int i_size, int i_chunkCount) {
        chunksDownloaded = new boolean[i_chunkCount];
        downloadSize = i_size;
        bytesDownloadedAlready = 0;
        chunksDownloadedAlready = 0;
    }

    int getPercentDownloaded(){
        double percent = (double)bytesDownloadedAlready / (double)downloadSize * 100;
        return (int)percent;
    }

}
