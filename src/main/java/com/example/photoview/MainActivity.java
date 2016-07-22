package com.example.photoview;

import android.app.Dialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String filePath;
    private RecyclerView mRecyclerView;
    private PhotoViewAdapter mPhotoViewAdapter;
    private List<Map<String, String>> mediaList = new ArrayList<>();
    private List<Map<String, String>> imageList = new ArrayList<>();
    private List<Map<String, String>> gifList = new ArrayList<>();
    DividerGridItemDecoration mDividerGridItemDecoration;

    String[] imagesColums = new String[]{
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
    };
    String[] mediaThumbColumns = new String[]{
            MediaStore.Video.Thumbnails.DATA,
            MediaStore.Video.Thumbnails.VIDEO_ID
    };
    String[] mediaColumns = new String[]{
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
//        initData();
//        scanImage();
//        scanVideo();

        scanGif();
    }

    private void scanGif() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                scanGif("gif", filePath);
                if (gifList.size() > 0) {
                    Log.i("slack", "size:" + gifList.size());
                    if (gifList.size() <= 4) {
                        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                        mRecyclerView.removeItemDecoration(mDividerGridItemDecoration);
                        mDividerGridItemDecoration = new DividerGridItemDecoration(MainActivity.this);
                        mRecyclerView.addItemDecoration(mDividerGridItemDecoration);
                    }
                    mPhotoViewAdapter = new PhotoViewAdapter(mRecyclerView, gifList);
                    mRecyclerView.setAdapter(mPhotoViewAdapter);
                }
            }
        }).start();
    }

    // 根据文件夹路径读取里面的 gif 图片
    private void scanGif(String type, String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        File[] files = file.listFiles();
        Map<String, String> map;
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory()) {
                    if (isType(type, f.getName())) {
                        map = new HashMap<>();
                        map.put("pic_path", f.getAbsolutePath());
                        map.put("file_type", type);
                        gifList.add(map);
//                        Log.i("slack","path " +  f.getPath() + "    " +
//                        f.getAbsolutePath());
                    }
                } else {
                    scanGif(type, path + File.separator + f.getName());
                }
            }
        }
    }

    private boolean isType(String type, String name) {
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
            return false;
        }
        if (name.length() > (type.length() + 1)) {
            if (type.equals(name.substring(name.lastIndexOf(".") + 1))) {
                return true;
            }
        }
        return false;
    }

    //  缩略图路径 thumbPath 文件路径filePath
    private void scanVideo() {
        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                // SELECT * FROM TABLE limit 0, 1 SORT BY DATE_MODIFIED DESC
                return new CursorLoader(MainActivity.this,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaColumns, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC");
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (data != null && !data.isClosed() && data.getCount() > 0) {
                    Log.i("slack", "Count: " + data.getCount());
//                    data.moveToFirst();
                    String path;
                    Map<String, String> map;
                    File file;
                    while (data.moveToNext()) {
                        path = data.getString(data.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        map = new HashMap<>();
                        map.put("file_path", path);
//                        Log.i("slack", "filePath: " + path);
//                        Log.i("slack", "title: " + data.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE) );
                        //获取当前Video对应的Id，然后根据该ID获取其Thumb
                        int id = data.getInt(data.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                        Cursor thumbCursor = getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, mediaThumbColumns, MediaStore.Video.Thumbnails.VIDEO_ID + "=?", new String[]{id + ""}, null);

                        if (thumbCursor.moveToFirst()) {
                            path = thumbCursor.getString(thumbCursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA));
//                            Log.i("slack","thumbPath: " + path);
                            map.put("pic_path", path);
                        }
                        Log.i("slack", map.get("file_path") + "," + map.get("pic_path"));
                        mediaList.add(map);
                    }
                    data.close();
                    System.gc();
                } else {
                    Log.i("slack", "no data");
                }
                Log.i("slack", "mediaList: " + mediaList.size());
                mPhotoViewAdapter = new PhotoViewAdapter(mRecyclerView, mediaList);
                mRecyclerView.setAdapter(mPhotoViewAdapter);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
            }
        });
    }

    private void scanImage() {

        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                // SELECT * FROM TABLE limit 0, 1 SORT BY DATE_MODIFIED DESC
                return new CursorLoader(MainActivity.this,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imagesColums, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC");
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (data != null && !data.isClosed() && data.getCount() > 0) {
                    Log.i("slack", "Count: " + data.getCount());
                    data.moveToFirst();
                    String path;
                    Map<String, String> map;
                    File file;
                    while (data.moveToNext()) {
                        path = data.getString(data.getColumnIndex(MediaStore.Images.Media.DATA));
                        map = new HashMap<>();
                        map.put("pic_path", path);
                        imageList.add(map);
//                        Log.i("slack", "Path: " + path);
//                        Log.i("slack", "Date: " + data.getString(data.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)));
                    }
                    data.close();
                    System.gc();
                } else {
                    Log.i("slack", "no data");
                }
                Log.i("slack", "imageList: " + imageList.size());
                mPhotoViewAdapter = new PhotoViewAdapter(mRecyclerView, imageList);
                mRecyclerView.setAdapter(mPhotoViewAdapter);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
            }
        });
    }

    private void initData() {
//        ArrayList<String> mDatas = new ArrayList<String>();
//        for (int i = 'A'; i < 'z'; i++){
//            mDatas.add("" + (char) i);
//        }
//        mPhotoViewAdapter = new PhotoViewAdapter(this,mDatas);
    }

    TextView right;
    View cancleView;

    private void initView() {
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies";
        mDividerGridItemDecoration = new DividerGridItemDecoration(this, 2);
        mRecyclerView = (RecyclerView) findViewById(R.id.photo_recyclerview);
        //设置布局管理器
//        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));  // 现行管理器，支持横向、纵向。
//        mRecyclerView.setLayoutManager(new GridLayoutManager(this,4));  // 网格布局,没行4个
        // 瀑布流式的布局 VERTICAL代表有多少列   HORIZONTAL 就代表有多少行
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.HORIZONTAL));
        //设置adapter
//        mRecyclerView.setAdapter(mPhotoViewAdapter);

        //设置Item增加、移除动画
//        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        //添加分割线
        mRecyclerView.addItemDecoration(mDividerGridItemDecoration);

        right = (TextView) findViewById(R.id.head_rigth);
        cancleView = findViewById(R.id.cancle_view);
        cancleView.setVisibility(View.GONE);
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.i("slack","click...right");
                if (cancleView.getVisibility() == View.GONE) {
                    cancleView.setVisibility(View.VISIBLE);
                    right.setText("删除");
                    mPhotoViewAdapter.operation = true;
                } else {
                    Log.i("slack", "click...del");
                    deleteSelect();
                }
            }
        });
        findViewById(R.id.cancle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.i("slack","click...cancle");
                cancleSelect();

            }
        });
    }

    private void cancleSelect() {
        cancleView.setVisibility(View.GONE);
        right.setText("操作");
        mPhotoViewAdapter.cancleSelect();
    }

    private void deleteSelect() {
        new AlertDialog.Builder(this).setMessage("确认删除这些吗?")
                .setTitle("温馨提示")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < mPhotoViewAdapter.selectPath.length; i++) {
                                    if (!TextUtils.isEmpty(mPhotoViewAdapter.selectPath[i])) {
                                        mPhotoViewAdapter.removeData(i);
                                        new File(mPhotoViewAdapter.selectPath[i]).delete();
                                    }
                                }
                            }
                        }).start();
                        cancleSelect();
                        dialog.dismiss();
                    }
                }).setNegativeButton("取消", null).show();

    }
}
