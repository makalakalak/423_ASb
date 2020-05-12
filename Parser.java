import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Parser {
    static String outputDir = "C:\\Users\\Jorda\\Documents\\Uni\\2020\\COMP423\\ASp2\\";
    static String remove = "[^0-9a-z #+_]";
    static String replace = "[/(){}\\[\\]\\|@,;]";
    static long seed = 0;
    static double threshhold = 0.01;

    public static void main(String[] args) {
        File p1 = new File(outputDir + "\\p1.csv"); // you will obviously have to change this.
        File p2a = new File(outputDir + "\\p2a.csv"); // you will obviously have to change this.
        File p2b = new File(outputDir + "\\p2b.tsv"); // you will obviously have to change this.
        File p3 = new File(outputDir + "\\p3.csv"); // you will obviously have to change this.
        File p4 = new File(outputDir + "\\p4.csv"); // you will obviously have to change this.
        File p5 = new File(outputDir + "\\p5.csv"); // you will obviously have to change this.

        MersenneTwisterFast mtf = new MersenneTwisterFast(seed);

        HashMap<Long, Instance> instances = new HashMap<>();

        try {
            BufferedReader bf = new BufferedReader(new FileReader(p1));

            Scanner scan;
            String line;
            Sentiment sentiment;

            // ###############################################################

            HashMap<Long, Instance> instances1 = new HashMap<>();

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

                String resTweet = getTweet(scan.nextLine());

                sentiment = getSentimentP1(rawSentiment);
                if(sentiment != null && addThisInstance(mtf))
                    instances1.put(id, new Instance(resTweet, sentiment));
            }

            System.out.println("fin s1: " + instances1.size());

            // ###############################################################

            bf = new BufferedReader(new FileReader(p2a));
            bf.readLine(); // skip header
            HashMap<Long, String> tempMap = new HashMap<>(); // id:tweet map
            HashMap<Long, Instance> instances2 = new HashMap<>();
            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter(",");

                String s = scan.next();
                s = s.replaceAll("\"", "");
                Long id = Long.valueOf(s);

                String resTweet = getTweet(scan.nextLine());

                tempMap.put(id, resTweet);
            }

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

                    sentiment = getSentimentP2(neg, neu, pos);
                    if(sentiment != null  && addThisInstance(mtf))
                        instances2.put(id, new Instance(tempMap.get(id), sentiment));
                }
            }

            System.out.println("fin s2: " + instances2.size());

            // ###############################################################

            bf = new BufferedReader(new FileReader(p3));
            bf.readLine(); // skip header
            HashMap<Long, Instance> instances3 = new HashMap<>();

            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter(",");

                while(!scan.hasNextLong()){
                    line = bf.readLine();
                    scan = new Scanner(line);
                    scan.useDelimiter(",");
                }

                Long id = Long.valueOf(scan.next());

                scan.next(); // _golden
                scan.next(); // _unit_state
                scan.next(); // _trusted_judgements
                scan.next(); // _last_judgement

                if(!scan.hasNextInt()) {
                    scan.nextLine();
                    continue;
                }
                int s = Integer.valueOf(scan.next());
                sentiment = getSentimentP3(s);

                scan.next(); // sentiment confidence
                scan.next(); // date
                scan.next(); // id
                scan.next(); // query
                scan.next(); // sentiment gold

                String resTweet;
                while (!scan.hasNext()) {
                    line = bf.readLine();
                    scan = new Scanner(line);
                    scan.next();
                }
                resTweet = getTweet(scan.nextLine());

                if(sentiment != null && addThisInstance(mtf))
                    instances3.put(id, new Instance(resTweet, sentiment));
            }

            System.out.println("fin s3: " + instances3.size());

            // ###############################################################

            bf = new BufferedReader(new FileReader(p4));
            bf.readLine(); // skip header

            HashMap<Long, Instance> instances4 = new HashMap<>();
            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter(",");

                Long id = Long.valueOf(scan.next()) * 928; // random ID modifier to avoid duplicates

                sentiment = getSentimentP4(scan.next().toLowerCase());
                scan.next(); // author
                String resTweet = getTweet(scan.nextLine());

                if(sentiment != null  && addThisInstance(mtf))
                    instances4.put(id, new Instance(resTweet, sentiment));
            }

            System.out.println("fin s4: " + instances4.size());

            // ###############################################################

            bf = new BufferedReader(new FileReader(p5));
            bf.readLine(); // skip header
            HashMap<Long, Instance> instances5 = new HashMap<>();

            while((line = bf.readLine()) != null){
                scan = new Scanner(line);
                scan.useDelimiter(",");

                long id = scan.nextLong();

                sentiment = getSentimentP5(scan.nextInt());

                String resTweet = getTweet(scan.nextLine());

                if(sentiment != null && addThisInstance(mtf))
                    instances5.put(id, new Instance(resTweet, sentiment));
            }

            System.out.println("fin s5: " + instances5.size());

            // ###############################################################

            instances.putAll(instances1); instances.putAll(instances2); instances.putAll(instances3);
            instances.putAll(instances4); instances.putAll(instances5);

            System.out.println("complete: " + instances.size());

            BufferedWriter bw = new BufferedWriter(new FileWriter((new File(outputDir + "tweetCollection.csv"))));

            bw.write("id, tweet, sentiment\n");

            for(Map.Entry<Long, Instance> entry: instances.entrySet()){
                long id = entry.getKey();
                Instance i = entry.getValue();

                line = id + ", " + i.tweet + ", " + i.s.sent + "\n";

                bw.write(line);
            }

            bw.close();
        } catch(Exception ioe) {
            ioe.printStackTrace();
        }
    }

    public static boolean addThisInstance(MersenneTwisterFast mtf){
        double v = mtf.nextDouble(true, true);
        if(Double.compare(threshhold, v) > 0) return true;
        return false;
    }

    public static String getTweet(String rawTweet){
        String res = rawTweet.toLowerCase();
        res = res.replaceAll(remove, "");
        res = res.replaceAll(replace, " ");

        String resTweet = "";
        Scanner tweetScan = new Scanner(res);
        String token;
        while(tweetScan.hasNext()){
            token = tweetScan.next();
            if(!token.contains("http") && !token.equals("rt")) resTweet += " " + token;
        }

        return resTweet;
    }

    public static Sentiment getSentimentP1(int rawSentiment){
        int res;

        // relatively arbitrary assignments, but hey.
        // Ignore all borderline instances.
        if(rawSentiment == 4)
            res = 1;
        else if(rawSentiment == 2)
            res = 0;
        else if(rawSentiment == 0)
            res = -1;
        else
            return null;

        return new Sentiment(res);
    }

    public static Sentiment getSentimentP2(double rawNeg, double rawNeu, double rawPos){
        int res;

        // relatively arbitrary assignments, but hey.
        // Ignore all borderline instances.
        if(Double.compare(rawPos, rawNeu) > 0 && Double.compare(rawPos, rawNeg) > 0)
            res = 1;
        else if(Double.compare(rawNeu, rawPos) > 0 && Double.compare(rawNeu, rawNeg) > 0)
            res = 0;
        else if(Double.compare(rawNeg, rawPos) > 0 && Double.compare(rawNeg, rawNeu) > 0)
            res = -1;
        else
            return null;

        return new Sentiment(res);
    }

    public static Sentiment getSentimentP3(int rawSentiment){
        int res;

        // relatively arbitrary assignments, but hey.
        // per the Kaggle 0-5 class assignments
        if(rawSentiment == 5)
            res = 1;
        else if(rawSentiment == 3)
            res = 0;
        else if(rawSentiment == 1)
            res = -1;
        else return null;

        return new Sentiment(res);
    }

    public static Sentiment getSentimentP4(String rawSentiment){
        int res;

        // my subjective distinction of positive/neutral/negative
        // class boundaries between the below words. Intentionally
        // didn't get too detailed.

        rawSentiment = rawSentiment.replaceAll("\"", "");

        if(rawSentiment.matches("enthusiasm|fun|happiness|love"))
            res = 1;
        else if(rawSentiment.matches("neutral|relief|surprise|empty|boredom"))
            res = 0;
        else if(rawSentiment.matches("sadness|worry|hate|anger"))
            res = -1;
        else
            return null;

        return new Sentiment(res);
    }

    public static Sentiment getSentimentP5(int rawSentiment){
        int res;

        if(rawSentiment == 1) res = 1;
        else if(rawSentiment == 0) res = -1;
        else return null;

        return new Sentiment(res);
    }


    private static class Instance{
        public String tweet;
        public Sentiment s;

        public Instance(String text, Sentiment sentiment){
            tweet = text; s = sentiment;
        }
    }

    private static class Sentiment{
        public int sent;

        public Sentiment(int s){
            sent = s;
        }
    }
}



