package peergos.server;

import peergos.shared.corenode.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.mutable.*;
import peergos.shared.storage.*;
import peergos.shared.user.*;

import java.util.*;
import java.util.concurrent.*;

public class RegisteredUserKeyFilter {

    private final CoreNode core;
    private final MutablePointers mutable;
    private final ContentAddressedStorage dht;
    private final Map<PublicKeyHash, Boolean> allowedKeys = new ConcurrentHashMap<>();

    public RegisteredUserKeyFilter(CoreNode core, MutablePointers mutable, ContentAddressedStorage dht) {
        this.core = core;
        this.mutable = mutable;
        this.dht = dht;
    }

    private void reloadKeys() {
        try {
            List<String> usernames = core.getUsernames("").get();
            Set<PublicKeyHash> updated = new HashSet<>();
            for (String username : usernames) {
                updated.addAll(WriterData.getOwnedKeysRecursive(username, core, mutable, dht));
            }
            Set<PublicKeyHash> toRemove = new HashSet<>();
            for (PublicKeyHash hash : allowedKeys.keySet())
                if (! updated.contains(hash))
                    toRemove.add(hash);
            for (PublicKeyHash hash : toRemove)
                allowedKeys.put(hash, false);
            for (PublicKeyHash hash : updated)
                allowedKeys.put(hash, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isAllowed(PublicKeyHash signerHash) {
        Boolean value = allowedKeys.get(signerHash);
        if (value != null)
            return value;
        // slow reload
        reloadKeys();
        return allowedKeys.getOrDefault(signerHash, false);
    }
}
