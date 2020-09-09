package com.automattic.simplenote;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.Ghost;

import java.util.Calendar;
import java.util.HashMap;

public class LastSyncTimeCache {
    private HashMap<String, Calendar> mSyncTimes = new HashMap<>();
    private HashMap<String, Boolean> mIsSynced = new HashMap<>();
    private SyncTimeListener mListener;

    public Calendar getLastSyncTime(String key) {
        return mSyncTimes.get(key);
    }

    public boolean isSynced(String key) {
        Boolean knownSyncStatus = mIsSynced.get(key);

        return knownSyncStatus != null
            ? knownSyncStatus
            : true;
    }

    public void addListener(SyncTimeListener listener) {
        mListener = listener;
    }

    private void notifyListener(String entityId) {
        if (null != mListener) {
            mListener.onUpdate(entityId, getLastSyncTime(entityId), isSynced(entityId));
        }
    }

    private void updateSyncTime(String entityId) {
        Calendar now = Calendar.getInstance();
        mSyncTimes.put(entityId, now);

        notifyListener(entityId);
    }

    private void updateIsSynced(Bucket<Note> bucket, String entityId) {
        Note local;
        Ghost ghost;

        try {
            local = bucket.get(entityId);
            ghost = bucket.getGhost(entityId);
        } catch (Exception e) {
            // this is not an error we are going to handle at this level
            // so skip it if something uncorrectable happens
            return;
        }

        try {
            boolean isSame = (
                local.getContent().equals(ghost.getDiffableValue().getString("content"))
            );

            mIsSynced.put(entityId, isSame);
            notifyListener(entityId);
        } catch (Exception e) {
            // also don't care about errors here
            // either we can compare the bucket and ghost data or we can't
        }
    }

    public Bucket.Listener<Note> listener = new Bucket.Listener<Note>() {
        @Override
        public void onSaveObject(Bucket<Note> bucket, Note object) {
            mIsSynced.put(object.getSimperiumKey(), false);
            notifyListener(object.getSimperiumKey());
        }

        @Override
        public void onNetworkChange(Bucket<Note> bucket, Bucket.ChangeType type, String entityId) {
            if (null == entityId) {
                // no key for bucket operations like RESET, INDEX
                return;
            }

            updateSyncTime(entityId);
            updateIsSynced(bucket, entityId);
        }

        @Override
        public void onDeleteObject(Bucket<Note> bucket, Note object) {

        }

        @Override
        public void onBeforeUpdateObject(Bucket<Note> bucket, Note object) {

        }
    };

    interface SyncTimeListener {
        void onUpdate(String entityId, Calendar lastSyncTime, boolean isSynced);
    }
}
