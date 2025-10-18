package com.example.mobil2025.data.repo;

import com.example.mobil2025.model.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Repozitorijum koji upravlja korisnicima u Firestore bazi.
 * Obezbeđuje da je korisničko ime jedinstveno (transakcija).
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
        // kljuc za username, sve mala slova
        String unameKey = username.toLowerCase(Locale.ROOT);

        DocumentReference unameRef = db.collection("usernames").document(unameKey);
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot unameSnap = transaction.get(unameRef);
                    if (unameSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "Korisničko ime je zauzeto",
                                FirebaseFirestoreException.Code.ALREADY_EXISTS
                        );
                    }

                    // rezerviši username
                    Map<String, Object> unameDoc = new HashMap<>();
                    unameDoc.put("uid", uid);
                    transaction.set(unameRef, unameDoc);

                    // kreiraj profil
                    UserProfile profile = new UserProfile(
                            uid,
                            email,
                            username,
                            avatarKey,
                            System.currentTimeMillis()
                    );
                    transaction.set(userRef, profile);

                    return null;
                }).addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }
}
