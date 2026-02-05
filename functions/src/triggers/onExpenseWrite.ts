import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

export const onExpenseWrite = onDocumentWritten(
  {
    document: "groups/{groupId}/expenses/{expenseId}",
    region: "europe-west1",
    maxInstances: 10,
  },
  async (event) => {
    // 1. Setup & Data Extraction
    const change = event.data;
    if (!change) return;

    const afterData = change.after.exists ? change.after.data() : null;
    const beforeData = change.before.exists ? change.before.data() : null;

    if (!afterData) {
      return;
    }

    const expenseId = event.params.expenseId;

    // 2. Math Validation
    const amount = Number(afterData.amount) || 0;
    const splits = afterData.splits || {};
    let splitsSum = 0;
    for (const key in splits) {
      if (Object.prototype.hasOwnProperty.call(splits, key)) {
        splitsSum += Number(splits[key]);
      }
    }

    // Check difference (allow 0.02 for float errors)
    const isMathValid = Math.abs(amount - splitsSum) < 0.02;

    // 3. Infinite Loop Protection & Status Update
    const currentStatus = afterData.is_math_valid;

    if (currentStatus !== isMathValid) {
      logger.info(`Updating status for ${expenseId}: ${isMathValid}`);
      await change.after.ref.update({
        is_math_valid: isMathValid,
        server_validated_at: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // 4. Audit Log (History)
    const changes: Record<string, unknown> = {};
    let hasBusinessChanges = false;

    if (!beforeData) {
      hasBusinessChanges = true;
      changes["_event"] = "CREATED";
      changes["description"] = afterData.description;
      changes["amount"] = afterData.amount;
      changes["payers"] = afterData.payers;
      changes["splits"] = afterData.splits;
    } else {
      const ignoreFields = [
        "is_math_valid",
        "server_validated_at",
        "updated_at",
      ];

      for (const key in afterData) {
        if (Object.prototype.hasOwnProperty.call(afterData, key)) {
          if (JSON.stringify(beforeData[key]) !==
              JSON.stringify(afterData[key])) {
            if (!ignoreFields.includes(key)) {
              hasBusinessChanges = true;
            }
            changes[key] = {from: beforeData[key], to: afterData[key]};
          }
        }
      }
    }

    if (!hasBusinessChanges) {
      return;
    }

    // Write to History
    await change.after.ref.collection("history").add({
      action: beforeData ? "UPDATE" : "CREATE",
      changes: changes,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      is_math_valid: isMathValid,
    });

    const groupId = event.params.groupId;
    await admin.firestore().collection("groups").doc(groupId).update({
      updated_at: Date.now(),
    });
  }
);
