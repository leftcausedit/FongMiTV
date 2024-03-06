package com.fongmi.android.tv.ui.presenter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;
import androidx.palette.graphics.Palette;

import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.databinding.AdapterFuncBinding;

public class FuncPresenter extends Presenter {

    private final OnClickListener mListener;

    public FuncPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Func item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterFuncBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Func item = (Func) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        if (item.getId() > 0) holder.binding.getRoot().setId(item.getId());
        holder.binding.text.setText(item.getText());
        holder.binding.icon.setImageResource(item.getDrawable());
        holder.binding.getRoot().setClipToOutline(false); // tested clipToOutLine, clipToPadding, clipChildren; this is to enable shadow of the childView in viewHolder, can't set it in xml, may because Presenter will override it;
        setHolderBackground(holder);
        if (item.getNextFocusLeft() > 0) holder.binding.getRoot().setNextFocusLeftId(item.getNextFocusLeft());
        if (item.getNextFocusRight() > 0) holder.binding.getRoot().setNextFocusRightId(item.getNextFocusRight());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    private void setHolderBackground(ViewHolder holder) {
        Bitmap origin = ((BitmapDrawable) ((Activity) mListener).getWindow().getDecorView().getBackground()).getBitmap();
        Palette p = Palette.from(origin).generate();
        int lightColor = p.getLightVibrantColor(0);
        int darkColor = p.getDarkMutedColor(0);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{lightColor, darkColor});
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(16f);
        holder.binding.background.setBackground(drawable);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterFuncBinding binding;

        public ViewHolder(@NonNull AdapterFuncBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}