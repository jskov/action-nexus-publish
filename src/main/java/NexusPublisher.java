class NexusPublisher {
    public static void main(String[] args) {
        System.out.println("Args: " + args);
        System.out.println("Environment:");
        System.getenv().entrySet().stream()
            .map(e -> " '" + e.getKey() + "=" + e.getValue() + "'")
            .sorted()
            .forEach(System.out::println);
    }
}