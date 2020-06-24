public class Controller {
    private static ImageCrawler imageCrawler;

    public static void main(String[] args) {

        imageCrawler = new ImageCrawler();
        imageCrawler.run();

    }
}
