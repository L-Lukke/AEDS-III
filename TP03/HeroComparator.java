import java.util.Comparator;

public class HeroComparator {

    // Comparator by ID
    public static Comparator<Hero> byId() {
        return Comparator.comparingInt(Hero::getId);
    }

    // Comparator by Name
    public static Comparator<Hero> byName() {
        return Comparator.comparing(Hero::getName, Comparator.naturalOrder());
    }

    // Comparator by Identity
    public static Comparator<Hero> byIdentity() {
        return Comparator.comparing(Hero::getIdentity, Comparator.naturalOrder());
    }

    // Comparator by Alignment
    public static Comparator<Hero> byAlignment() {
        return Comparator.comparing(Hero::getAlignment, Comparator.naturalOrder());
    }

    // Comparator by Eye Color
    public static Comparator<Hero> byEyeColor() {
        return Comparator.comparing(Hero::getEyeColor, Comparator.naturalOrder());
    }

    // Comparator by Hair Color
    public static Comparator<Hero> byHairColor() {
        return Comparator.comparing(Hero::getHairColor, Comparator.naturalOrder());
    }

    // Comparator by Gender
    public static Comparator<Hero> byGender() {
        return Comparator.comparing(Hero::getGender, Comparator.naturalOrder());
    }

    // Comparator by Status
    public static Comparator<Hero> byStatus() {
        return Comparator.comparing(Hero::getStatus, Comparator.naturalOrder());
    }

    // Comparator by Appearances
    public static Comparator<Hero> byAppearances() {
        return Comparator.comparingInt(Hero::getAppearances);
    }

    // Comparator by First Appearance (Date)
    public static Comparator<Hero> byFirstAppearance() {
        return Comparator.comparing(Hero::getFirstAppearance, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    // Comparator by Year
    public static Comparator<Hero> byYear() {
        return Comparator.comparingInt(Hero::getYear);
    }

    // Comparator by Universe
    public static Comparator<Hero> byUniverse() {
        return Comparator.comparing(Hero::getUniverse, Comparator.naturalOrder());
    }
}