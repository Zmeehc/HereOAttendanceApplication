package com.llavore.hereoattendance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.llavore.hereoattendance.utils.ProductDetails;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ItemHolderView> {
    private Context context;
    private List<ProductDetails> list;

    public ProductAdapter(Context context, List<ProductDetails> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ProductAdapter.ItemHolderView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_details, parent, false);
        return new ItemHolderView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.ItemHolderView holder, int position) {
        ProductDetails currentItem  = list.get(position);

        Glide.with(context)
                        .load(currentItem.getProductImg())
                                .placeholder(R.drawable.applogo)
                                        .into(holder.productImage);

        holder.productName.setText(currentItem.getProductName());
        holder.productPrice.setText(currentItem.getProductPrice());

    }

    @Override
    public int getItemCount() {
        return list.size();
    }



    public class ItemHolderView extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice;
        public ItemHolderView(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);

        }
    }
}
