import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

// Prevent multiple initializations
if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Claims a "Ghost" member in a group, linking history to the current user.
 * Input: { groupId: string, ghostId: string }
 * Output: { success: boolean }
 */
export const claimGhost = onCall(
  {region: "europe-west1", maxInstances: 10},
  async (request) => {
    // 1. Auth Check
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be logged in.");
    }
    const uid = request.auth.uid;
    const {groupId, ghostId} = request.data;

    if (!groupId || !ghostId) {
      throw new HttpsError("invalid-argument", "Missing groupId or ghostId.");
    }

    // 2. Fetch Group
    const groupRef = db.collection("groups").doc(groupId);
    const groupSnap = await groupRef.get();

    if (!groupSnap.exists) {
      throw new HttpsError("not-found", "Group not found.");
    }

    const groupData = groupSnap.data();

    // Check Membership
    const members = (groupData?.members as string[]) || [];
    if (!members.includes(uid)) {
      throw new HttpsError(
        "permission-denied",
        "You are not a member of this group."
      );
    }

    // 3. Verify Ghost Logic
    const ghosts = groupData?.ghosts || {};
    const targetGhost = ghosts[ghostId];

    if (!targetGhost) {
      throw new HttpsError("not-found", "Ghost member not found.");
    }

    if (targetGhost.is_merged) {
      throw new HttpsError(
        "failed-precondition",
        "This ghost is already claimed by someone else."
      );
    }

    // 4. Batch Execution
    const batch = db.batch();
    const userRef = db.collection("users").doc(uid);

    // Update Group: Mark ghost as merged
    batch.update(groupRef, {
      [`ghosts.${ghostId}.is_merged`]: true,
      [`ghosts.${ghostId}.merged_with_uid`]: uid,
      updated_at: Date.now(),
    });

    // Update User: Add ghost ID to linked list
    batch.set(userRef, {
      linked_ghost_ids: admin.firestore.FieldValue.arrayUnion(ghostId),
      updated_at: Date.now(),
    }, {merge: true});

    await batch.commit();

    return {success: true};
  }
);
