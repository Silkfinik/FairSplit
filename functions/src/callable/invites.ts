import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

// Prevent multiple initializations
if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

// Alphabet for code generation (excluding ambiguous chars: I, 1, 0, O)
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 6;

/**
 * Helper to generate a random 6-char code.
 * @return {string} Random 6-char alphanumeric string.
 */
function generateRandomCode(): string {
  let result = "";
  for (let i = 0; i < CODE_LENGTH; i++) {
    const randomIndex = Math.floor(Math.random() * ALPHABET.length);
    result += ALPHABET[randomIndex];
  }
  return result;
}

/**
 * Creates a short invite code for a group.
 * Input: { groupId: string }
 * Output: { code: string }
 */
export const createInviteCode = onCall(
  {region: "europe-west1", maxInstances: 10},
  async (request) => {
    // 1. Auth Check
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be logged in.");
    }
    const uid = request.auth.uid;
    const groupId = request.data.groupId;

    if (!groupId) {
      throw new HttpsError("invalid-argument", "Group ID is required.");
    }

    // 2. Permission Check: User must be a member of the group
    const groupRef = db.collection("groups").doc(groupId);
    const groupSnap = await groupRef.get();

    if (!groupSnap.exists) {
      throw new HttpsError("not-found", "Group not found.");
    }

    const groupData = groupSnap.data();
    const members = (groupData?.members as string[]) || [];

    if (!members.includes(uid)) {
      throw new HttpsError(
        "permission-denied",
        "You are not a member of this group."
      );
    }

    // 3. Check if group already has a code (Optimization)
    if (groupData?.invite_code) {
      return {code: groupData.invite_code};
    }

    // 4. Generate Unique Code (Collision Handling)
    let code = "";
    let isUnique = false;
    let attempts = 0;

    const invitesCollection = db.collection("invites");

    while (!isUnique && attempts < 5) {
      code = generateRandomCode();
      try {
        const existing = await invitesCollection.doc(code).get();
        if (!existing.exists) {
          isUnique = true;
        }
      } catch (e) {
        console.error("Error checking code uniqueness", e);
      }
      attempts++;
    }

    if (!isUnique) {
      throw new HttpsError(
        "resource-exhausted",
        "Failed to generate a unique code. Try again."
      );
    }

    // 5. Save Mapping (Code -> GroupID) & Update Group
    const batch = db.batch();
    const inviteRef = invitesCollection.doc(code);

    batch.set(inviteRef, {
      groupId: groupId,
      createdBy: uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    batch.update(groupRef, {
      invite_code: code,
    });

    await batch.commit();

    return {code: code};
  }
);

/**
 * Joins a group using an invite code.
 * Input: { code: string }
 * Output: { groupId: string, groupName: string }
 */
export const joinByInviteCode = onCall(
  {region: "europe-west1", maxInstances: 10},
  async (request) => {
    // 1. Auth Check
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be logged in.");
    }
    const uid = request.auth.uid;
    const code = request.data.code?.toUpperCase(); // Case insensitive

    if (!code || code.length !== CODE_LENGTH) {
      throw new HttpsError("invalid-argument", "Invalid invite code.");
    }

    // 2. Find Invite
    const inviteSnap = await db.collection("invites").doc(code).get();
    if (!inviteSnap.exists) {
      throw new HttpsError("not-found", "Invite code not found.");
    }

    const groupId = inviteSnap.data()?.groupId;

    // 3. Add User to Group
    const groupRef = db.collection("groups").doc(groupId);

    // We use arrayUnion to safely add without duplicates
    await groupRef.update({
      members: admin.firestore.FieldValue.arrayUnion(uid),
    });

    // 4. Return Group Info (so client can navigate immediately)
    const groupSnap = await groupRef.get();
    const groupName = groupSnap.data()?.name || "Group";

    return {
      groupId: groupId,
      groupName: groupName,
    };
  }
);
