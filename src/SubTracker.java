package dako;

import java.io.*;
import java.net.URL;
import java.text.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.GregorianCalendar;
import java.util.Calendar;

public class SubTracker {
    static BufferedWriter logger;
    static File log = new File("log.txt");
    static BufferedWriter errorLogger;
    static File errorLog = new File("errors.txt");
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static String currentRun = sdf.format(new Date(System.currentTimeMillis()));

    final static int MAX_TRIES = 5;
    final static int FIELD_LENGTH1 = 16;
    final static int FIELD_LENGTH2 = 30;
    
    public static void main(String args[]) throws IOException {
        // program requires at least one argument on execution
        if (args.length==0) return;
        // dry run, display results in console without saving anything.
        if (args[0].equals("test")) { 
            System.out.println(args[1]+" has "+getSubs(args[1])+" subscribers.");
            return;
        }
        
        // setting up files for storing data for future runs.
        
        // auto check file
        File channelFile = new File("channels.txt");
        if (!channelFile.exists()) channelFile.createNewFile();
        // directory of saved data
        File dir = new File("channels");
        if (!dir.exists()) dir.mkdir(); // create directory where data over checked channels are saved.
        // open writers to error and log files. create files if they do not already exist
        try {
            if (!errorLog.exists()) errorLog.createNewFile(); // create error log if one does not exist
            errorLogger = new BufferedWriter(new FileWriter(errorLog,true));
            if (!log.exists()) log.createNewFile(); // create log file if one does not exist
            logger = new BufferedWriter(new FileWriter(log,true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // check every channel written down in local file "channels.txt"
        if (args[0].equals("$auto$")) { 
            String s = "";
            try {
                Scanner in = new Scanner(channelFile);
                while (in.hasNextLine()) {
                    s += in.nextLine()+" ";
                }
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                errorLogger.write(e.toString()+"\n");
            }
            s.substring(0,s.length()-1);
            args = s.split(" +");
        }
        logger.write(currentRun+": "+"acquiring subscriber amount for channel(s): ");
        logger.write(Arrays.toString(args)+"\n");
        for (String s : args) {
            boolean res = update(s);
            if (!res) logger.write(currentRun+": "+"check on channel "+s+" failed\n");
        }
        logger.write(currentRun+": "+"End run\n\n");
        logger.close();
        errorLogger.close();
    }

    private static boolean update(String channel) throws IOException {
        try {
            int subs = getSubs(channel);
            int tries = 0;
            while (subs ==-1) {
                if (tries++ == MAX_TRIES) return false;
                subs = getSubs(channel);
            }
            File f = new File("channels/"+channel+"_subs.txt");
            if (!f.exists()) {
                f.createNewFile();
            }
            Scanner fis = new Scanner(f);
            String lastLine ="";
            while (fis.hasNextLine()) {
                lastLine = fis.nextLine();
            }
            fis.close();
            String reg = "(\\d+-\\d+-\\d+ \\d+:\\d+:\\d+) +(\\d+)";
            Matcher m = Pattern.compile(reg).matcher(lastLine);
            if (m.find()) {
                long lastDate = sdf.parse(m.group(1), new ParsePosition(0)).getTime();
                Date d = new Date();
                d.setTime(lastDate);
                int lastSubs = Integer.parseInt(m.group(2));
                newEntry(f,subs,lastDate,lastSubs);
            } else {
                newEntry(f,channel,subs);
            }

        } catch (Exception e) {
            errorLogger.write(currentRun+" Update: "+e.toString() + "\n");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private static void newEntry(File f, int subs, long lastD, int lastS) throws IOException {
        try {
            Long current = System.currentTimeMillis();
            Long diffD = current-lastD;
            int diffS = subs-lastS;
            BufferedWriter fos = new BufferedWriter(new FileWriter(f,true));
            fos.write("\n");
            String subString = String.format(
                "%1$-26s%2$-"+FIELD_LENGTH1+"s",sdf.format(new Date(current)),subs
            );
            fos.write(subString);
            String change = (diffS>=0?"+":"")+diffS;
            change = String.format("%1$-"+FIELD_LENGTH1+"s",change);
            fos.write(change);
            Double diff = new Double(diffS)/new Double(diffD/1000);
            if (diff!=0) {
                DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                dfs.setDecimalSeparator('.');
                String str1 = (diffS>=0?"+":"")+new DecimalFormat("#.#####",dfs).format(diff*60)+" per minute";
                str1 = String.format("%1$-"+FIELD_LENGTH2+"s",str1);
                fos.write(str1);
                String str2;
                if (Math.abs(diff)<1) {
                    str2 = (diffS>=0?"+":"-")+"1/"+new DecimalFormat("#.##",dfs).format(Math.pow(Math.abs(diff),-1));
                } else {
                    str2 = (diffS>=0?"+":"-")+new DecimalFormat("#.##",dfs).format(diff);
                }
                fos.write("~"+str2+" per second");
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            errorLogger.write(currentRun+" NewEntry1: "+e.toString()+"\n");
        }
    }

    private static void newEntry(File f,String title, int subs) throws IOException {
        try {
            Calendar cal = new GregorianCalendar();
            Long current = System.currentTimeMillis();
            BufferedWriter fos = new BufferedWriter(new FileWriter(f,true));
            fos.write("***Youtube subscriber count for channel "+title+"***\n\n");
            String out = String.format("%1$-26s","TIMESTAMP (GMT "+cal.getTimeZone().getRawOffset()/3600000+")");
            fos.write(out);
            fos.write(String.format(
                "%1$-"+FIELD_LENGTH1+"s%2$-"+FIELD_LENGTH1+
                "s%3$-"+FIELD_LENGTH2+"s%4$s"
                ,"SUBSCRIBERS","CHANGE","PER MINUTE","PER SECOND")+"\n"
            );
            fos.write(String.format("%1$-26s%2$s",sdf.format(new Date(current)),subs));
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            errorLogger.write(currentRun+" NewEntry2: "+e.toString()+"\n");
        }
    }

    private static int getSubs(String channel) throws IOException {
        System.out.println("Finding subs for "+channel);
        StringBuffer buffer = new StringBuffer();
        String line;
        URL url = null;
        try {
            url = new URL("https://www.youtube.com/user/"+channel+"/about");
            System.out.println("URL acquired");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            System.out.println("URL stream opened to buffered reader");
            int timeout=10;
            while(!br.ready() && timeout-->0) {
                System.out.println("waiting for buffer to ready");
                try {
                Thread.sleep(500);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            while((line = br.readLine()) != null) {
                buffer.append(line + "\n");
            }
            System.out.println("Closing buffer, content moved to string");
            String text = buffer.toString();
            br.close();
            Matcher m = Pattern.
                    compile("about-stat\"><b>(.*)</b>").
                    matcher(text);
            if (m.find()) {
                String s = m.group(1).replaceAll("\\D","");
                System.out.println("Sub-count: "+s);
                return Integer.parseInt(s);
            } else {
                System.out.println("pattern not found");
                errorLogger.write("Regex Pattern not found, youtube source code might have changed.");
            }

        } catch (Exception e) {
            System.out.println("In getSubs");
            e.printStackTrace();
            errorLogger.write(currentRun+" GetSubs: "+e.toString() + "\n");
        }
        return -1;
    }
}
