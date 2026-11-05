package io.hamlook.aetheria.features.dungeons.rooms.secret;

import java.util.List;

public final class SecretRoom {

    private final String key;
    private final List<SecretWaypoint> secrets;
    private final int secretCount;
    private final int expectedSecrets;

    private volatile boolean allSecretsCleared;

    private volatile int minX;
    private volatile int minZ;
    private volatile int maxX;
    private volatile int maxZ;
    private volatile boolean boundsValid;

    public SecretRoom(String key, List<SecretWaypoint> secrets, int expectedSecrets) {
        this.key = key;
        this.secrets = secrets;
        this.expectedSecrets = expectedSecrets;
        this.secretCount = countSecrets(secrets);
    }

    private static int countSecrets(List<SecretWaypoint> secrets) {
        int count = 0;
        for (SecretWaypoint secret : secrets) {
            if (secret.category().isSecret()) count++;
        }
        return count;
    }

    public int secretCount() {
        return secretCount;
    }

    public int expectedSecrets() {
        return expectedSecrets;
    }

    public String key() {
        return key;
    }

    public List<SecretWaypoint> secrets() {
        return secrets;
    }

    public void setBounds(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        boundsValid = true;
    }

    // room bounds include the wall blocks, so allow one extra block on the far side
    public boolean contains(double x, double z) {
        return boundsValid && x >= minX && x <= maxX + 1 && z >= minZ && z <= maxZ + 1;
    }

    public boolean hasBounds() {
        return boundsValid;
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxZ() {
        return maxZ;
    }

    // same center the map draws the room name and tick around
    public double centerX() {
        return (minX + maxX) / 2.0 + 0.5;
    }

    public double centerZ() {
        return (minZ + maxZ) / 2.0 + 0.5;
    }

    public boolean allSecretsCleared() {
        return allSecretsCleared;
    }

    public void setAllSecretsCleared(boolean allSecretsCleared) {
        this.allSecretsCleared = allSecretsCleared;
    }
}
