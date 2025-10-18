package com.example.mobil2025.data.repo;

import com.example.mobil2025.model.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Firestore repozitorijum:
 * - Varijanta sa jedinstvenim korisničkim imenom (rezervacija u "usernames/{uname}")
 * - Upis profila u "users/{uid}" sa enabled=false + activationDeadline=24h
 */
public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void createUserProfileWithUniqueUsername(
            String uid,
            String email,
            String username,
            String avatarKey,
            OnSuccessListener<Void> ok,
            OnFailureListener err
    ) {
        String unameKey = username.toLowerCase(Locale.ROOT);

        DocumentReference unameRef = db.collection("usernames").document(unameKey);
        DocumentReference userRef  = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Jedinstvenost username-a
                    DocumentSnapshot unameSnap = transaction.get(unameRef);
                    if (unameSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "Korisničko ime je zauzeto",
                                FirebaseFirestoreException.Code.ALREADY_EXISTS
                        );
                    }

                    long now = System.currentTimeMillis();
                    long activationDeadline = now + 24L * 60L * 60L * 1000L; // 24h u ms

                    // Rezerviši username
                    Map<String, Object> unameDoc = new HashMap<>();
                    unameDoc.put("uid", uid);
                    transaction.set(unameRef, unameDoc);

                    // Kreiraj profil (enabled=false dok se email ne verifikuje)
                    UserProfile profile = new UserProfile(uid, email, username, avatarKey, now);
                    profile.enabled = false;
                    transaction.set(userRef, profile);

                    // Dodatna meta polja (nije u modelu)
                    Map<String, Object> extra = new HashMap<>();
                    extra.put("activationDeadline", activationDeadline);
                    transaction.set(userRef, extra, SetOptions.merge());

                    return null;
                }).addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    /**
     * (Opciono) Jednostavnija varijanta BEZ kolekcije "usernames" – samo users/{uid}.
     * Pozovi umesto gornje metode ako ne želiš jedinstvenost username-a u posebnoj kolekciji.
     */
    public void createUserProfile(
            String uid,
            String email,
            String username,
            String avatarKey,
            OnSuccessListener<Void> ok,
            OnFailureListener err
    ) {
        DocumentReference userRef = db.collection("users").document(uid);

        long now = System.currentTimeMillis();
        long activationDeadline = now + 24L * 60L * 60L * 1000L;

        UserProfile profile = new UserProfile(uid, email, username, avatarKey, now);
        profile.enabled = false;

        Map<String, Object> extra = new HashMap<>();
        extra.put("activationDeadline", activationDeadline);

        userRef.set(profile)
                .continueWithTask(t -> userRef.set(extra, SetOptions.merge()))
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }
}
