import java.util.Scanner;  // Import the Scanner class

//Importing all the types of the game as well as the type chart
class Main {
    //This is set up to make sure types are entered and converted properly to match
    private static String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }

    public static double offensiveMatchup(String playerType, String opponentType) {
        String p = normalizeType(playerType);
        String o = normalizeType(opponentType);

        if (p.equals("NORMAL")) {
            if (o.equals("ROCK") || o.equals("STEEL")) return 0.5;
            if (o.equals("GHOST")) return 0;
            return 1;
        } else if (p.equals("FIRE")) {
            if (o.equals("GRASS") || o.equals("ICE") || o.equals("BUG") || o.equals("STEEL")) return 2;
            if (o.equals("FIRE") || o.equals("WATER") || o.equals("ROCK") || o.equals("DRAGON")) return 0.5;
            return 1;
        } else if (p.equals("WATER")) {
            if (o.equals("FIRE") || o.equals("GROUND") || o.equals("ROCK")) return 2;
            if (o.equals("WATER") || o.equals("GRASS") || o.equals("DRAGON")) return 0.5;
            return 1;
        } else if (p.equals("GRASS")) {
            if (o.equals("WATER") || o.equals("GROUND") || o.equals("ROCK")) return 2;
            if (o.equals("FIRE") || o.equals("GRASS") || o.equals("POISON") || o.equals("FLYING")
                    || o.equals("BUG") || o.equals("DRAGON") || o.equals("STEEL")) return 0.5;
            return 1;
        } else if (p.equals("ELECTRIC")) {
            if (o.equals("WATER") || o.equals("FLYING")) return 2;
            if (o.equals("ELECTRIC") || o.equals("GRASS") || o.equals("DRAGON")) return 0.5;
            if (o.equals("GROUND")) return 0;
            return 1;
        } else if (p.equals("ICE")) {
            if (o.equals("GRASS") || o.equals("GROUND") || o.equals("FLYING") || o.equals("DRAGON")) return 2;
            if (o.equals("FIRE") || o.equals("WATER") || o.equals("ICE") || o.equals("STEEL")) return 0.5;
            return 1;
        } else if (p.equals("FIGHTING")) {
            if (o.equals("NORMAL") || o.equals("ICE") || o.equals("ROCK") || o.equals("DARK") || o.equals("STEEL")) return 2;
            if (o.equals("POISON") || o.equals("FLYING") || o.equals("PSYCHIC") || o.equals("BUG") || o.equals("FAIRY")) return 0.5;
            if (o.equals("GHOST")) return 0;
            return 1;
        } else if (p.equals("POISON")) {
            if (o.equals("GRASS") || o.equals("FAIRY")) return 2;
            if (o.equals("POISON") || o.equals("GROUND") || o.equals("ROCK") || o.equals("GHOST")) return 0.5;
            if (o.equals("STEEL")) return 0;
            return 1;
        } else if (p.equals("GROUND")) {
            if (o.equals("FIRE") || o.equals("ELECTRIC") || o.equals("POISON") || o.equals("ROCK") || o.equals("STEEL")) return 2;
            if (o.equals("GRASS") || o.equals("BUG")) return 0.5;
            if (o.equals("FLYING")) return 0;
            return 1;
        } else if (p.equals("FLYING")) {
            if (o.equals("GRASS") || o.equals("FIGHTING") || o.equals("BUG")) return 2;
            if (o.equals("ELECTRIC") || o.equals("ROCK") || o.equals("STEEL")) return 0.5;
            return 1;
        } else if (p.equals("PSYCHIC")) {
            if (o.equals("FIGHTING") || o.equals("POISON")) return 2;
            if (o.equals("PSYCHIC") || o.equals("STEEL")) return 0.5;
            if (o.equals("DARK")) return 0;
            return 1;
        } else if (p.equals("BUG")) {
            if (o.equals("GRASS") || o.equals("PSYCHIC") || o.equals("DARK")) return 2;
            if (o.equals("FIRE") || o.equals("FIGHTING") || o.equals("POISON") || o.equals("FLYING")
                    || o.equals("GHOST") || o.equals("STEEL") || o.equals("FAIRY")) return 0.5;
            return 1;
        } else if (p.equals("ROCK")) {
            if (o.equals("FIRE") || o.equals("ICE") || o.equals("FLYING") || o.equals("BUG")) return 2;
            if (o.equals("FIGHTING") || o.equals("GROUND") || o.equals("STEEL")) return 0.5;
            return 1;
        } else if (p.equals("GHOST")) {
            if (o.equals("PSYCHIC") || o.equals("GHOST")) return 2;
            if (o.equals("DARK")) return 0.5;
            if (o.equals("NORMAL")) return 0;
            return 1;
        } else if (p.equals("DRAGON")) {
            if (o.equals("DRAGON")) return 2;
            if (o.equals("STEEL")) return 0.5;
            if (o.equals("FAIRY")) return 0;
            return 1;
        } else if (p.equals("DARK")) {
            if (o.equals("PSYCHIC") || o.equals("GHOST")) return 2;
            if (o.equals("FIGHTING") || o.equals("DARK") || o.equals("FAIRY")) return 0.5;
            return 1;
        } else if (p.equals("STEEL")) {
            if (o.equals("ICE") || o.equals("ROCK") || o.equals("FAIRY")) return 2;
            if (o.equals("FIRE") || o.equals("WATER") || o.equals("ELECTRIC") || o.equals("STEEL")) return 0.5;
            return 1;
        } else if (p.equals("FAIRY")) {
            if (o.equals("FIGHTING") || o.equals("DRAGON") || o.equals("DARK")) return 2;
            if (o.equals("FIRE") || o.equals("POISON") || o.equals("STEEL")) return 0.5;
            return 1;
        }

        return 1;
    }
    //Defensive match up is the same as offensive matchup but with the player and opponent types reversed
    public static double defensiveMatchup(String playerType, String opponentType) {
        return offensiveMatchup(opponentType, playerType);
    }
    //Enter in the pokemon you want to compare and their types here. I was the doing the 1v1 format so I only have 3 pokemon in the code but you can add more if you want to.
    static String[] Kingambit = {"DARK", "STEEL"};
    static String[] Sneasler = {"POISON", "FIGHTING"};
    static String[] Ursaluna = {"GROUND", "NORMAL"};

    //Function that calculates the offensive value  for each 1v1 
    public static double bestOffensiveTotal(String[] myPokemon, String[] oppPokemon) {
        double total1 = 1;
        double total2 = 1;
        for (int l = 0; l < 2; l++) {
            total1 *= offensiveMatchup(myPokemon[0], oppPokemon[l]);
        }
        for (int j = 0; j < 2; j++) {
            total2 *= offensiveMatchup(myPokemon[1], oppPokemon[j]);
        }
        return total1 > total2 ? total1 : total2;
    }
    //Function that calculates the defensive value for each 1v1
    public static double bestDefensiveTotal(String[] myPokemon, String[] oppPokemon) {
        double total1 = 1;
        double total2 = 1;
        for (int i = 0; i < 2; i++) {
            total1 *= defensiveMatchup(myPokemon[i], oppPokemon[0]);
        }
        for (int k = 0; k < 2; k++) {
            total2 *= defensiveMatchup(myPokemon[k], oppPokemon[1]);
        }
        return total1 > total2 ? total1 : total2;
    }
    
    public static void main(String[] args) {
        //Add in the number of pokemon you want to compare here by adding more arrays and scanners
        String[] pokemon1 = {"", ""};
        String[] pokemon2 = {"", ""};
        String[] pokemon3 = {"", ""};
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the first type of the first mon:");
        pokemon1[0]= scanner.nextLine();
        System.out.println("Enter the second type of the first mon:");
        pokemon1[1]= scanner.nextLine();
        System.out.println("Enter the first type of the second mon:");
        pokemon2[0]= scanner.nextLine();        
        System.out.println("Enter the second type of the second mon:");
        pokemon2[1]= scanner.nextLine();
        System.out.println("Enter the first type of the third mon:");
        pokemon3[0]= scanner.nextLine();
        System.out.println("Enter the second type of the third mon:");
        pokemon3[1]= scanner.nextLine();

        //Calculetes total offensive value base on the 3 1v1 matchups from bestOffensiveTotal and prints them out
        double UrsalunaOffense = bestOffensiveTotal(Ursaluna, pokemon1) * bestOffensiveTotal(Ursaluna, pokemon2) * bestOffensiveTotal(Ursaluna, pokemon3);
        double KingambitOffense = bestOffensiveTotal(Kingambit, pokemon1) * bestOffensiveTotal(Kingambit, pokemon2) * bestOffensiveTotal(Kingambit, pokemon3);
        double SneaslerOffense = bestOffensiveTotal(Sneasler, pokemon1) * bestOffensiveTotal(Sneasler, pokemon2) * bestOffensiveTotal(Sneasler, pokemon3);

        System.out.println("Ursaluna Offense: " + UrsalunaOffense);
        System.out.println("Kingambit Offense: " + KingambitOffense);
        System.out.println("Sneasler Offense: " + SneaslerOffense);

        //Calculates total defensive value based on the 3 1v1 matchups from bestDefensiveTotal and prints them out
        double UrsalunaOverall = UrsalunaOffense / (bestDefensiveTotal(Ursaluna, pokemon1) * bestDefensiveTotal(Ursaluna, pokemon2) * bestDefensiveTotal(Ursaluna, pokemon3));
        double KingambitOverall = KingambitOffense / (bestDefensiveTotal(Kingambit, pokemon1) * bestDefensiveTotal(Kingambit, pokemon2) * bestDefensiveTotal(Kingambit, pokemon3));
        double SneaslerOverall = SneaslerOffense / (bestDefensiveTotal(Sneasler, pokemon1) * bestDefensiveTotal(Sneasler, pokemon2) * bestDefensiveTotal(Sneasler, pokemon3));

        System.out.println("Ursaluna Overall: " + UrsalunaOverall);
        System.out.println("Kingambit Overall: " + KingambitOverall);
        System.out.println("Sneasler Overall: " + SneaslerOverall);

        // for both cases the higher the number the better.
    }
}
