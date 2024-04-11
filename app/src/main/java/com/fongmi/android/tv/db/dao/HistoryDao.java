package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.fongmi.android.tv.bean.History;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class HistoryDao extends BaseDao<History> {

    @Query("SELECT * FROM History WHERE cid = :cid ORDER BY createTime DESC")
    public abstract List<History> find(int cid);

    @Query("SELECT * FROM History WHERE cid = :cid AND `key` = :key")
    public abstract History find(int cid, String key);

    @Query("SELECT * FROM History WHERE cid = :cid AND vodName = :vodName")
    public abstract List<History> findByName(int cid, String vodName);

    @Query("DELETE FROM History WHERE cid = :cid AND `key` = :key")
    public abstract void delete(int cid, String key);

    @Query("DELETE FROM History WHERE cid = :cid")
    public abstract void delete(int cid);

    @Query("DELETE FROM History")
    public abstract void delete();

    @Query("SELECT * FROM History")
    public abstract List<History> findAll();

    @Transaction
    public void insertAndKeepNewest(List<History> items) {
        List<Long> result = insert(items);
        List<History> list = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) == -1) {
                History item = items.get(i);
                if (item.getCreateTime() > find(item.getCid(), item.getKey()).getCreateTime()) {
                    list.add(item);
                }
            }
        }
        if (list.size() > 0) update(list);
    }
}
