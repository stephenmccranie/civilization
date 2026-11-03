package dev.civilization;

/** The server fixture dev.ps1 selected, and its phase; empty when this run is not that fixture. */
final class Fixture {
    static String phase(String name) {
        return name.equals(System.getProperty("civilization.fixture")) ? System.getProperty("civilization.fixturePhase", "") : "";
    }
}
