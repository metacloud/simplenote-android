package com.automattic.simplenote;

import com.simperium.client.Bucket;
import com.simperium.client.Syncable;

import java.util.Calendar;
import java.util.HashMap;

public class LastSyncTimeCache<T extends Syncable> {
    private HashMap<String, Calendar> mSyncTimes = new HashMap<>();
    private SyncTimeListener mListener;

    public Calendar getLastSyncTime(String key) {
        return mSyncTimes.get(key);
    }

    public void addListener(SyncTimeListener listener) {
        mListener = listener;
    }

    private void updateSyncTime(String entityId) {
        Calendar now = Calendar.getInstance();
        mSyncTimes.put(entityId, now);

        if (null != mListener) {
            mListener.onUpdate(entityId, now);
        }
    }

    public Bucket.Listener<T> listener = new Bucket.Listener<T>() {
        @Override
        public void onSaveObject(Bucket<T> bucket, T object) {
            updateSyncTime(object.getSimperiumKey());
        }

        @Override
        public void onNetworkChange(Bucket<T> bucket, Bucket.ChangeType type, String entityId) {
            if (null == entityId) {
                // no key for bucket operations like RESET, INDEX
                return;
            }

            updateSyncTime(entityId);
        }

        @Override
        public void onDeleteObject(Bucket<T> bucket, T object) {

        }

        @Override
        public void onBeforeUpdateObject(Bucket<T> bucket, T object) {

        }
    };

    interface SyncTimeListener {
        void onUpdate(String entityId, Calendar lastSyncTime);
    }
}
