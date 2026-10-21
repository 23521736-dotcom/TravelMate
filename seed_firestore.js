const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');
if (!fs.existsSync(serviceAccountPath)) {
  console.error('Missing serviceAccountKey.json');
  process.exit(1);
}

const seedPath = path.join(__dirname, 'app', 'src', 'main', 'assets', 'data', 'seed_data.json');
const seed = JSON.parse(fs.readFileSync(seedPath, 'utf8'));

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath)),
});

const firestore = admin.firestore();

async function seedCollection(collectionName, records) {
  if (!records) return 0;

  const entries = Object.entries(records);
  for (let i = 0; i < entries.length; i += 450) {
    const batch = firestore.batch();
    entries.slice(i, i + 450).forEach(([id, value]) => {
      batch.set(firestore.collection(collectionName).doc(id), { id, ...value }, { merge: true });
    });
    await batch.commit();
  }
  return entries.length;
}

async function seedUserNotifications(records) {
  if (!records) return 0;

  let count = 0;
  const batch = firestore.batch();
  Object.entries(records).forEach(([userId, notifications]) => {
    Object.entries(notifications || {}).forEach(([notificationId, value]) => {
      batch.set(
        firestore.collection('notifications').doc(userId).collection('items').doc(notificationId),
        { id: notificationId, userId, ...value },
        { merge: true },
      );
      count += 1;
    });
  });
  if (count > 0) await batch.commit();
  return count;
}

async function seedTrips(records) {
  if (!records) return { trips: 0, memberships: 0 };

  let trips = 0;
  let memberships = 0;
  const batch = firestore.batch();

  Object.entries(records).forEach(([tripId, trip]) => {
    const finalTrip = { id: tripId, ...trip };
    batch.set(firestore.collection('trips').doc(tripId), finalTrip, { merge: true });
    trips += 1;

    const userId = finalTrip.userId;
    if (userId) {
      batch.set(
        firestore.collection('userTrips').doc(userId).collection('trips').doc(tripId),
        { tripId, joinedAt: finalTrip.createdAt || Date.now() },
        { merge: true },
      );
      batch.set(
        firestore.collection('tripMembers').doc(tripId).collection('members').doc(userId),
        {
          userId,
          tripId,
          role: 'OWNER',
          status: 'JOINED',
          joinedAt: finalTrip.createdAt || Date.now(),
          userName: seed.users?.[userId]?.fullName || seed.users?.[userId]?.displayName || 'Traveler',
          userPhotoUrl: seed.users?.[userId]?.photoUrl || null,
        },
        { merge: true },
      );
      memberships += 1;
    }

    if (finalTrip.inviteCode) {
      batch.set(firestore.collection('inviteCodes').doc(finalTrip.inviteCode), { tripId }, { merge: true });
    }
    if (finalTrip.shareToken) {
      batch.set(firestore.collection('shareTokens').doc(finalTrip.shareToken), { tripId }, { merge: true });
    }
  });

  await batch.commit();
  return { trips, memberships };
}

async function run() {
  const users = await seedCollection('users', seed.users);
  const destinations = await seedCollection('destinations', seed.destinations);
  const expenses = await seedCollection('expenses', seed.expenses);
  const notifications = await seedUserNotifications(seed.notifications);
  const tripResult = await seedTrips(seed.trips);

  console.log(`Seeded ${users} users.`);
  console.log(`Seeded ${destinations} destinations.`);
  console.log(`Seeded ${expenses} expenses.`);
  console.log(`Seeded ${notifications} notifications.`);
  console.log(`Seeded ${tripResult.trips} trips and ${tripResult.memberships} trip memberships.`);
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
