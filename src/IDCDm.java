import java.io.IOException;
import java.io.File;
import java.io.*;


public class IDCDm {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
//        DownloadManager manager;
//        if(args.length == 2){
//             manager = new DownloadManager(args[0], Integer.parseInt(args[1]));
//        }
//        else{
//            manager = new DownloadManager(args[0]);
//        }
//        manager.downloadFile();
//        String url = "https://pbs.twimg.com/media/D034qFyW0AAO8JQ.jpg";
        String url = "https://archive.org/download/Mario1_500/Mario1_500.avi";
        DownloadManager manager = new DownloadManager(url,7);
        manager.downloadFile();
//        test_metadata();

    }

    private static void test_metadata() throws IOException, ClassNotFoundException{
        String file = "C:/Users/URISHAP/Desktop/netoworks_lab/download_metadata";

        FileInputStream fis = new FileInputStream(new File(file));
        ObjectInputStream ois = new ObjectInputStream(fis);
        Metadata status = (Metadata) ois.readObject();
        BufferedWriter writer = new BufferedWriter(new FileWriter("TEST_FILE_2"));
        for(int i = 0; i < status.downloadStatus.length; i++){
            long current = i * 4096;
            writer.write("chunk " + current + " downloaded: " + status.downloadStatus[i] + "\n");
        }
        System.out.println("total status size " + status.downloadStatus.length);
        ois.close();
        writer.flush();
        writer.close();
    }
}

/* TODO:
 * 1. Add decision about whether to go for maximum concurrent connections or less based on file size
 * 2. delete metadata files and other things that come with download
 * 3. download percent as a variable that is printed during the run
 * 4. if download "breaks" - continue from last spot (metadata file probably)
 * 5. add logic if max concurrent is not given as input
 * 6. Print end result (success/ fail ...)
 * 7. input is a file with multiple urls - download from multiple servers (??????????)
 */
