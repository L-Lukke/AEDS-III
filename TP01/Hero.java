import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Hero implements Comparable<Hero> {

    // Attributes
    private int id; // Unique identifier for the hero
    private String name; // Name of the hero
    private String identity; // Secret or alternate identity
    private String alignment; // Hero's alignment (Good, Neutral, Evil)
    private String eyeColor; // Eye color of the hero
    private String hairColor; // Hair color of the hero
    private String gender; // Gender of the hero
    private String status; // Current status of the hero (Living or Dead)
    private int appearances; // Number of issues the hero has appeared in
    private Date firstAppearance; // Date of the hero's first appearance
    private int year; // Year of the hero's first appearance
    private String universe; // Universe or setting where the hero exists

    // Default constructor
    public Hero() {}

    // Parameterized constructor to initialize all attributes
    public Hero(int id, String name, String identity, String alignment,
                String eyeColor, String hairColor, String gender, String status, 
                int appearances, Date firstAppearance, int year, String universe) {
        this.id = id;
        this.name = name;
        this.identity = identity;
        this.alignment = alignment;
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.gender = gender;
        this.status = status;
        this.appearances = appearances;
        this.firstAppearance = firstAppearance;
        this.year = year;
        this.universe = universe;
    }

    // Getter and setter methods
    public int getId() { return id; }
    public String getName() { return name; }
    public String getIdentity() { return identity; }
    public String getAlignment() { return alignment; }
    public String getEyeColor() { return eyeColor; }
    public String getHairColor() { return hairColor; }
    public String getGender() { return gender; }
    public String getStatus() { return status; }
    public int getAppearances() { return appearances; }
    public Date getFirstAppearance() { return firstAppearance; }
    public int getYear() { return year; }
    public String getUniverse() { return universe; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIdentity(String identity) { this.identity = identity; }
    public void setAlignment(String alignment) { this.alignment = alignment; }
    public void setEyeColor(String eyeColor) { this.eyeColor = eyeColor; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }
    public void setGender(String gender) { this.gender = gender; }
    public void setStatus(String status) { this.status = status; }
    public void setAppearances(int appearances) { this.appearances = appearances; }
    public void setFirstAppearance(Date firstAppearance) { this.firstAppearance = firstAppearance; }
    public void setYear(int year) { this.year = year; }
    public void setUniverse(String universe) { this.universe = universe; }

    // Serialize the Hero object to a byte array
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Formatting date as "dd/MM/yyyy"
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        
        dos.writeInt(this.getId());
        dos.writeUTF(this.getName());
        dos.writeUTF(this.getIdentity());
        dos.writeUTF(this.getAlignment());
        dos.writeUTF(this.getEyeColor());
        dos.writeUTF(this.getHairColor());
        dos.writeUTF(this.getGender());
        dos.writeUTF(this.getStatus());
        dos.writeInt(this.getAppearances());
        dos.writeUTF(df.format(this.getFirstAppearance())); // Serialize date as string
        dos.writeInt(this.getYear());
        dos.writeUTF(this.getUniverse());

        return baos.toByteArray();
    }

    // Deserialize the byte array to a Hero object
    public static Hero fromByteArray(byte ba[]) throws IOException {

        Hero hero = new Hero();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));

        hero.setId(dis.readInt());
        hero.setName(dis.readUTF());
        hero.setIdentity(dis.readUTF());;
        hero.setAlignment(dis.readUTF());
        hero.setEyeColor(dis.readUTF());
        hero.setHairColor(dis.readUTF());
        hero.setGender(dis.readUTF());
        hero.setStatus(dis.readUTF());
        hero.setAppearances(dis.readInt());

        try {
            hero.setFirstAppearance(stringToDate(dis.readUTF())); // Deserialize date string to Date object
        } catch (Exception e) {
            e.printStackTrace();
        }

        hero.setYear(dis.readInt());
        hero.setUniverse(dis.readUTF());

        return hero;
    }

    @Override
    public String toString() {
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        
        // Prepare the base string representation of the Hero object
        StringBuilder heroString = new StringBuilder("{");
        
        // Check and format the name, removing asterisks and appending the suffix if necessary
        String displayName = name.contains("*") ? name.replace("*", "") + "(Created by User)" : name;
        heroString.append("ID: ").append(id)
                .append(", name: ").append(!displayName.equals("ni") ? displayName : "not informed")
                .append(", identity: ").append(!identity.equals("ni") ? identity : "not informed")
                .append(", alignment: ").append(!alignment.equals("ni") ? alignment : "not informed")
                .append(", eyeColor: ").append(!eyeColor.equals("ni") ? eyeColor : "not informed")
                .append(", hairColor: ").append(!hairColor.equals("ni") ? hairColor : "not informed")
                .append(", gender: ").append(!gender.equals("ni") ? gender : "not informed")
                .append(", status: ").append(!status.equals("ni") ? status : "not informed")
                .append(", appearances: ").append(appearances != -1 ? appearances : "not informed")
                .append(", firstAppearance: ").append(firstAppearance != null && !df.format(firstAppearance).equals("11/11/1111") ? df.format(firstAppearance) : "not informed")
                .append(", universe: ").append(!universe.equals("ni") ? universe : "not informed")
                .append('}');
        
        return heroString.toString();
    }

    // Static method to populate a Hero object from a CSV line
    public static void read(String line, Hero hero) throws Exception {
        try {
            String[] aux = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            // Default empty values to "ni" (not informed)
            for(int i = 0; i < 12; i++) if (aux[i].equals("")) aux[i] = "ni";
            
            for(int i = 0; i < 12; i++) if (aux[i].contains("\"")) aux[i] = removeQuotes(aux[i]);

            // Trim whitespace
            for(int i = 0; i < 12; i++) aux[i].trim();

            hero.setId(Integer.parseInt(aux[0]) - 100000);
            hero.setName(aux[1]);
            hero.setIdentity(aux[2]);
            hero.setAlignment(aux[3]);
            hero.setEyeColor(aux[4]);
            hero.setHairColor(aux[5]);
            hero.setGender(aux[6]);
            hero.setStatus(aux[7]);
            hero.setAppearances(aux[8].equals("ni") ? -1 : Integer.parseInt(aux[8]));
            hero.setFirstAppearance(stringToDate(aux[9]));
            hero.setYear(aux[10].equals("ni") ? -1 : Integer.parseInt(aux[10]));
            hero.setUniverse(aux[11]);

        } catch (Exception e) { 
            e.printStackTrace();
        }    
    }

    // Static method to convert date strings into Date objects
    public static Date stringToDate(String aux) throws ParseException {
        aux = removeQuotes(aux);
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy, MMMM", Locale.ENGLISH);

        if(aux.contains(" Holiday")) aux = aux.replace(" Holiday", " December");// If "Holiday" is given as a month, change to december

        if(aux.equals("ni")) aux = "11/11/1111"; // Defaults the date if it's not informed

        try {
            Date parsedDate = format2.parse(aux);
            SimpleDateFormat finalFormat = new SimpleDateFormat("dd/MM/yyyy");
            return finalFormat.parse("01/" + new SimpleDateFormat("MM/yyyy").format(parsedDate));
        } catch (ParseException e) {
            // Ignore and try the next format
        }

        return format1.parse(aux); // Fallback to the primary date format
    }

    // Utility method to remove quotes from strings
    public static String removeQuotes(String input) {
        if (input == null) return null;
        return input.replace("\"", "");
    }

    @Override
    public int compareTo(Hero o) {
        return this.id - o.getId();
    }
}