package dosvald.provjeraracuna.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class BannerView extends ImageView {

	public BannerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public BannerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BannerView(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Drawable drawable = getDrawable();
		if (drawable != null) {
			int w = MeasureSpec.getSize(widthMeasureSpec);
			int imageW = drawable.getIntrinsicWidth();
			int imageH = drawable.getIntrinsicHeight();
			if (imageW == -1 || imageH == -1)
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			else {
				setMeasuredDimension(w, w * imageH / imageW);
			}
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

}
