import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Trigger: onGroupWrite
 * Listens to: /groups/{groupId}
 * Logic: When members added, fetch profiles and add to member_profiles.
 */
export const onGroupWrite = onDocumentWritten(
  {
    document: "groups/{groupId}",
    region: "europe-west1",
    maxInstances: 10,
  },
  async (event) => {
    const change = event.data;
    if (!change) return;

    const afterData = change.after.data();
    const beforeData = change.before.data();

    if (!afterData) return;

    const members = (afterData.members as string[]) || [];
    const oldMembers = beforeData ?
      ((beforeData.members as string[]) || []) : [];

    // Find new members
    const newMembers = members.filter((uid) => !oldMembers.includes(uid));

    // Also check if member_profiles is missing for existing members
    const currentProfiles = afterData.member_profiles || {};
    const missingProfiles = members.filter((uid) => !currentProfiles[uid]);

    const uidsToFetch = [...new Set([...newMembers, ...missingProfiles])];

    if (uidsToFetch.length === 0) return;

    console.log(`Fetching profiles for ${uidsToFetch.length} members`);

    const updates: Record<string, unknown> = {};

    await Promise.all(uidsToFetch.map(async (uid) => {
      const userSnap = await db.collection("users").doc(uid).get();
      if (userSnap.exists) {
        const userData = userSnap.data();
        updates[`member_profiles.${uid}`] = {
          display_name: userData?.display_name || "",
          photo_url: userData?.photo_url || null,
        };
      } else {
        updates[`member_profiles.${uid}`] = {
          display_name: "Unknown",
          photo_url: null,
        };
      }
    }));

    if (Object.keys(updates).length > 0) {
      updates["updated_at"] = Date.now();
      await change.after.ref.update(updates);
    }
  }
);
