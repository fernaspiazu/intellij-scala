package example;

public class Main2 {
    public static void main(String[] args) throws ClassNotFoundException {
        System.out.println(org.apache.commons.compress.MemoryLimitException.class);
        System.out.println(org.apache.commons.math.ArgumentOutsideDomainException.class);
        System.out.println(Main1.class.getClassLoader().loadClass("org.apache.commons.text.AlphabetConverter"));
    }
}