const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfile(uid, overrides = {}) {
  return {
    userId: uid,
    email: `${uid}@example.com`,
    displayName: "Tiwee Viewer",
    photoURL: "",
    theme: "dark",
    favorites: ["ch_1"],
    recentlyViewed: ["ch_1"],
    allowAdultChannels: false,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Authenticated user can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(docRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    docRef.update({
      theme: "amoled",
      favorites: ["ch_1", "ch_2"],
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user cannot list users collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").get());
});

test("Cross-user read and write is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validProfile(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ theme: "amoled", updatedAt: new Date() }));
});

test("Body userId spoofing is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(BOB_UID))
  );
});

test("Ghost shadow field injection is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, { isAdmin: true }))
  );
});

test("Invalid theme enum is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, { theme: "neon" }))
  );
});

test("Oversized favorites array (>200) is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tooMany = Array.from({ length: 201 }, (_, i) => `ch_${i}`);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, { favorites: tooMany }))
  );
});

test("Immutable createdAt mutation is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(docRef.set(validProfile(ALICE_UID)));
  await assertFails(
    docRef.update({
      createdAt: new Date(Date.now() - 100000),
      updatedAt: new Date(),
    })
  );
});
