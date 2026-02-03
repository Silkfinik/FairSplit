import {onDocumentUpdated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Trigger: onUserUpdate
 * Listens to: /users/{userId}
 * Logic: When user updates profile, update info in all groups.
 */
export const onUserUpdate = onDocumentUpdated(
  {
    document: "users/{userId}",
    region: "europe-west1",
    maxInstances: 10,
  },
  async (event) => {
    const userId = event.params.userId;
    const after = event.data?.after.data();
    const before = event.data?.before.data();

    if (!after || !before) return;

    // Check if name or photo changed
    const nameChanged = after.display_name !== before.display_name;
    const photoChanged = after.photo_url !== before.photo_url;

    if (!nameChanged && !photoChanged) return;

    const newProfile = {
      display_name: after.display_name || "",
      photo_url: after.photo_url || null,
    };

    console.log(`User ${userId} updated profile. Syncing to groups...`);

    // Find all groups where this user is a member
    const groupsQuery = db.collection("groups")
      .where("members", "array-contains", userId);

    const snapshot = await groupsQuery.get();

    if (snapshot.empty) return;

    const batch = db.batch();

    snapshot.docs.forEach((doc) => {
      // Update specific key in the map using dot notation
      batch.update(doc.ref, {
        [`member_profiles.${userId}`]: newProfile,
      });
    });

    await batch.commit();
    console.log(`Updated profile in ${snapshot.size} groups.`);
  }
);
