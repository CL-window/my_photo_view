package com.example.photoview;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * <p>Description:  </p>
 * Created by slack on 2016/7/21 13:28 .
 */
public class PhotoViewAdapter extends RecyclerView.Adapter<PhotoViewAdapter.PhotoViewHolder>{
    private Context context;
    List<String> testData; // test
    List<Map<String,String>> data; //  title +  uri
    String[] fileType = {"video","photo","gif"};
    GifView gifView ;
    final PopupWindow popwindow ;
    float downX,downY;

    public PhotoViewAdapter(Context context, List<Map<String,String>> data) {
        this.context = context;
        this.data = data;
        gifView = new GifView(context);
        popwindow = new PopupWindow(gifView, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, false);
        popwindow.setAnimationStyle(R.style.popwin_anim_style); // 动画

    }
//    public PhotoViewAdapter(Context context, List<String> data) {
//        this.context = context;
//        this.testData = data;
//    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PhotoViewHolder(LayoutInflater.from(context).inflate(R.layout.photo_item,parent,false));
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        //缩
        holder.photoView.setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(data.get(position).get("pic_path")), 300, 300));
//        holder.photoInfo.setText(data.get(position).get("title").toString());

//        holder.photoInfo.setText(testData.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
//        return testData.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {

        ImageView photoView;
//        TextView photoInfo;
        public PhotoViewHolder(View itemView) {
            super(itemView);
            photoView = (ImageView) itemView.findViewById(R.id.photo_item_img);
//            photoInfo = (TextView)itemView.findViewById(R.id.photo_item_info);
            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
//                    Log.i("slack","click..." + pos + " " + testData.get(pos) );
                    Log.i("slack","click..." + pos + " " + data.get(pos) );

                }
            });
            photoView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int pos = getAdapterPosition();
                    Log.i("slack","onLongClick..." + pos + " " + data.get(pos) );
                    if(fileType[2].equals(data.get(pos).get("file_type"))){
                        // popwindow show
                        // 设置好参数之后再show
                        gifView.setMovieResource(data.get(pos).get("pic_path"));
//                        popwindow.showAsDropDown(view);
                        popwindow.showAtLocation(view,Gravity.CENTER,0,0);// 显示在整个屏幕的中央
                        return true;
                    }
                    return false ; // 调用 onClick 事件,dissmiss
                }
            });
            /**
             * 好坑呀，一旦按住时移动，ontouch事件就不执行了
             * 只能通过按住点的坐标来判断了
             */
            photoView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
//                    Log.i("slack","onTouch..." + motionEvent.getX() + "," + motionEvent.getY());
                    switch (motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            downX = motionEvent.getX();
                            downY = motionEvent.getY();
                            break;
                        case MotionEvent.ACTION_UP:
                            dissmissPoup();
                            break;
                        default:
                            if(isMove(motionEvent.getX(),motionEvent.getY()) ){
                                dissmissPoup();
                            }
                            break;
                    }

                    return false;
                }
            });

        }

    }

    private void dissmissPoup() {
        if( popwindow != null && popwindow.isShowing()){
            popwindow.dismiss();
        }
    }

    private boolean isMove(float x, float y) {
        if(Math.abs(x - downX) > 0 || Math.abs(y - downY) > 0){
            return  true;
        }
        return false;
    }
}
