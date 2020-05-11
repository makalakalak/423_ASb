import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Parser {
    static String outputDir = "C:\\Users\\Jorda\\Documents\\Uni\\2020\\COMP423\\ASp2\\";

    public static void main(String[] args) {
        File p1 = new File(outputDir + "\\p1.csv"); // you will obviously have to change this.
        File p2a = new File(outputDir + "\\p2a.csv"); // you will obviously have to change this.
        File p2b = new File(outputDir + "\\p2b.tsv"); // you will obviously have to change this.

        HashMap<Long, Instance> instances = new HashMap<>();

        try {
            BufferedReader bf = new BufferedReader(new FileReader(p1));

            Scanner scan;
            String line;
            Scanner tweetScan;
            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter(",");

                String s = scan.next();
                s = s.replaceAll("\"", "");
                int rawSentiment = Integer.valueOf(s);

                s = scan.next(); s = s.replaceAll("\"", "");
                Long id = Long.valueOf(s);

                scan.next(); // date
                scan.next(); // "NO_QUERY"
                scan.next(); // username

                String tweet = scan.nextLine();
                tweet = tweet.replaceAll("\"", "");
                tweet = tweet.replaceAll(",", "");

                String resTweet = "";
                tweetScan = new Scanner(tweet);
                String token;
                while(tweetScan.hasNext()){
                    token = tweetScan.next();
                    if(!token.contains("http")) resTweet += " " + token;
                }

                Sentiment sent = getSentiment(rawSentiment);
                instances.put(id, new Instance(resTweet, sent));
            }

            System.out.println("fin s1: " + instances.size());

            bf = new BufferedReader(new FileReader(p2a));
            bf.readLine(); // skip header
            HashMap<Long, String> tempMap = new HashMap<>(); // id:tweet map
            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter(",");

                String s = scan.next();
                s = s.replaceAll("\"", "");
                Long id = Long.valueOf(s);

                String tweet = scan.nextLine();
                tweet = tweet.replaceAll("\"", "");
                tweet = tweet.replaceAll(",", "");

                String resTweet = "";
                tweetScan = new Scanner(tweet);
                String token;
                while(tweetScan.hasNext()){
                    token = tweetScan.next();
                    if(!token.contains("http")) resTweet += " " + token;
                }

                if(!tweet.startsWith("RT"))
                    tempMap.put(id, resTweet);
            }

            System.out.println("fin s2");

            bf = new BufferedReader(new FileReader(p2b));
            bf.readLine(); // skip header

            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter("\t");

                Long id = Long.valueOf(scan.next());
                if(tempMap.containsKey(id)){
                    double neg = Double.valueOf(scan.next());
                    double neu = Double.valueOf(scan.next());
                    double pos = Double.valueOf(scan.next());

                    instances.put(id, new Instance(tempMap.get(id), new Sentiment(neg, neu, pos)));
                }
            }

            System.out.println("fin s3: " + instances.size());
            System.out.println("complete");

            BufferedWriter bw = new BufferedWriter(new FileWriter((new File(outputDir + "masterFile.csv"))));

            for(Map.Entry<Long, Instance> entry: instances.entrySet()){
                long id = entry.getKey();
                Instance i = entry.getValue();

                line = id + ", " + i.tweet + ", " + i.s.neg + ", " + i.s.neu + ", " + i.s.pos + "\n";

                bw.write(line);
            }

            bw.close();
        } catch(Exception ioe) {
            ioe.printStackTrace();
        }
    }

    public static Sentiment getSentiment(int rawSentiment){
        double neg, neu, pos; neg = neu = pos = 0;

        // relatively arbitrary assignments, but hey.
        // per the Kaggle 0-5 class assignments
        if(rawSentiment == 4){
            pos = 1;
        } else if(rawSentiment == 3){
            pos = 0.5;
            neu = 0.5;
        } else if(rawSentiment == 2){
            neu = 1;
        } else if(rawSentiment == 1){
            neu = 0.5;
            neg = 0.5;
        } else { // rawSentiment == 0
            neg = 1;
        }

        return new Sentiment(neg, neu, pos);
    }

    private static class Instance{
        public String tweet;
        public Sentiment s;

        public Instance(String text, Sentiment sentiment){
            tweet = text; s = sentiment;
        }
    }

    private static class Sentiment{
        public double neg;
        public double neu;
        public double pos;

        public Sentiment(double n1, double n2, double p){
            neg = n1; neu = n2; pos = p;
        }
    }
}



