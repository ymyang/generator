package org.example.generator.utils;

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 英文单复数转换
 * Transforms words (from singular to plural, from camelCase to under_score, etc.). I got bored of doing Real Work...
 *
 * @author chuyeow
 * @modified Winter Lau (http://www.oschina.net/)
 */


public class InflectorUtil {


    // Pfft, can't think of a better name, but this is needed to avoid the price of initializing the pattern on each call.
    private static final Pattern UNDERSCORE_PATTERN_1 = Pattern.compile("([A-Z]+)([A-Z][a-z])");
    private static final Pattern UNDERSCORE_PATTERN_2 = Pattern.compile("([a-z\\d])([A-Z])");


    private static List<RuleAndReplacement> plurals = new ArrayList<RuleAndReplacement>();
    private static List<RuleAndReplacement> singulars = new ArrayList<RuleAndReplacement>();
    private static List<String> uncountables = new ArrayList<String>();


    private static InflectorUtil instance; // (Pseudo-)Singleton instance.

    private InflectorUtil() {
        // Woo, you can't touch me.

        initialize();
    }

    private void initialize() {
        plural("$", "s");
        plural("s$", "s");
        plural("(ax|test)is$", "$1es");
        plural("(octop|vir)us$", "$1i");
        plural("(alias|status)$", "$1es");
        plural("(bu)s$", "$1es");
        plural("(buffal|tomat)o$", "$1oes");
        plural("([ti])um$", "$1a");
        plural("sis$", "ses");
        plural("(?:([^f])fe|([lr])f)$", "$1$2ves");
        plural("(hive)$", "$1s");
        plural("([^aeiouy]|qu)y$", "$1ies");
        plural("([^aeiouy]|qu)ies$", "$1y");
        plural("(x|ch|ss|sh)$", "$1es");
        plural("(matr|vert|ind)ix|ex$", "$1ices");
        plural("([m|l])ouse$", "$1ice");
        plural("(ox)$", "$1es");
        plural("(quiz)$", "$1zes");


        singular("s$", "");
        singular("(n)ews$", "$1ews");
        singular("([ti])a$", "$1um");
        singular("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis");
        singular("(^analy)ses$", "$1sis");
        singular("([^f])ves$", "$1fe");
        singular("(hive)s$", "$1");
        singular("(tive)s$", "$1");
        singular("([lr])ves$", "$1f");
        singular("([^aeiouy]|qu)ies$", "$1y");
        singular("(s)eries$", "$1eries");
        singular("(m)ovies$", "$1ovie");
        singular("(x|ch|ss|sh)es$", "$1");
        singular("([m|l])ice$", "$1ouse");
        singular("(bus)es$", "$1");
        singular("(o)es$", "$1");
        singular("(shoe)s$", "$1");
        singular("(cris|ax|test)es$", "$1is");
        singular("([octop|vir])i$", "$1us");
        singular("(alias|status)es$", "$1");
        singular("^(ox)es", "$1");
        singular("(vert|ind)ices$", "$1ex");
        singular("(matr)ices$", "$1ix");
        singular("(quiz)zes$", "$1");


//        irregular("person", "people");
//        irregular("man$", "men");
//        irregular("child", "children");
        irregular("sex", "sexes");
        irregular("move", "moves");


        uncountable(new String[]{"equipment", "information", "rice", "money", "species", "series", "fish", "sheep"});
    }


    public static InflectorUtil getInstance() {
        if (instance == null) {
            instance = new InflectorUtil();
        }
        return instance;
    }


    public String underscore(String camelCasedWord) {


        // Regexes in Java are fucking stupid...
        String underscoredWord = UNDERSCORE_PATTERN_1.matcher(camelCasedWord).replaceAll("$1_$2");
        underscoredWord = UNDERSCORE_PATTERN_2.matcher(underscoredWord).replaceAll("$1_$2");
        underscoredWord = underscoredWord.replace('-', '_').toLowerCase();


        return underscoredWord;
    }

	/**
	 * 单数变复数
	 */
    public String pluralize(String word) {
        if (uncountables.contains(word.toLowerCase())) {
            return word;
        }
        return replaceWithFirstRule(word, plurals);
    }

	/**
	 * 复数变单数
	 */
    public String singularize(String word) {
        if (uncountables.contains(word.toLowerCase())) {
            return word;
        }
        return replaceWithFirstRule(word, singulars);
    }


    private String replaceWithFirstRule(String word, List<RuleAndReplacement> ruleAndReplacements) {


        for (RuleAndReplacement rar : ruleAndReplacements) {
            String rule = rar.getRule();
            String replacement = rar.getReplacement();


            // Return if we find a match.
            Matcher matcher = Pattern.compile(rule, Pattern.CASE_INSENSITIVE).matcher(word);
            if (matcher.find()) {
                return matcher.replaceAll(replacement);
            }
        }
        return word;
    }


    public String tableize(String className) {
        return pluralize(underscore(className));
    }


    public <T> String tableize(Class<T> klass) {
        // Strip away package name - we only want the 'base' class name.
        String className = klass.getName().replace(klass.getPackage().getName() + ".", "");
        return tableize(className);
    }


    public static void plural(String rule, String replacement) {
        plurals.add(0, new RuleAndReplacement(rule, replacement));
    }


    public static void singular(String rule, String replacement) {
        singulars.add(0, new RuleAndReplacement(rule, replacement));
    }


    public static void irregular(String singular, String plural) {
        plural(singular, plural);
        singular(plural, singular);
    }


    public static void uncountable(String... words) {
        for (String word : words) {
            uncountables.add(word);
        }
    }

    public static String upperFirstLatter(String original, @Nullable String separator) {
        if (separator == null) {
            return original.substring(0, 1).toUpperCase() + original.substring(1);
        }
        StringBuilder res = new StringBuilder();
        for (String item : original.split(separator)) {
            res.append(item.substring(0, 1).toUpperCase());
            res.append(item.substring(1));
        }
        return res.toString();
    }
}


// Ugh, no open structs in Java (not-natively at least).
class RuleAndReplacement {
    private String rule;
    private String replacement;

    public RuleAndReplacement(String rule, String replacement) {
        this.rule = rule;
        this.replacement = replacement;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}