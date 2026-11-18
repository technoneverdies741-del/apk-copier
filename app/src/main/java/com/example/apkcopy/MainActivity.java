package com.example.apkcopy;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout list;
    ArrayList<ApplicationInfo> apps = new ArrayList<>();
    int blue = 0xff1769aa;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(24,24,24,16); root.setBackgroundColor(0xfff5f7fa);
        TextView title = new TextView(this); title.setText("APK Copier"); title.setTextSize(26); title.setTypeface(null, Typeface.BOLD); title.setTextColor(0xff17212b); root.addView(title);
        TextView desc = new TextView(this); desc.setText("Export installed app APK files for inspection. Select an app to save its APK or split APKs."); desc.setTextSize(15); desc.setTextColor(0xff52606d); desc.setPadding(0,8,0,14); root.addView(desc);
        Button refresh = new Button(this); refresh.setText("Refresh apps"); refresh.setOnClickListener(v -> loadApps()); root.addView(refresh);
        ScrollView sc = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(1); sc.addView(list); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); loadApps();
    }
    void loadApps() {
        list.removeAllViews(); apps.clear(); PackageManager pm=getPackageManager();
        List<ApplicationInfo> all=pm.getInstalledApplications(0);
        Collections.sort(all,(a,b)->pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));
        for(ApplicationInfo ai:all) {
            if(ai.packageName.equals(getPackageName())) continue; apps.add(ai);
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(12,9,4,9);
            LinearLayout info=new LinearLayout(this); info.setOrientation(1);
            TextView name=new TextView(this); name.setText(pm.getApplicationLabel(ai)); name.setTextColor(0xff17212b); name.setTextSize(16); name.setTypeface(null,Typeface.BOLD);
            TextView pkg=new TextView(this); pkg.setText(ai.packageName); pkg.setTextColor(0xff65727f); pkg.setTextSize(12);
            info.addView(name); info.addView(pkg); row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            Button copy=new Button(this); copy.setText("Export"); copy.setTextColor(blue); copy.setOnClickListener(v->exportApp(ai)); row.addView(copy);
            list.addView(row); View line=new View(this); line.setBackgroundColor(0xffdce2e8); list.addView(line,new LinearLayout.LayoutParams(-1,1));
        }
        Toast.makeText(this, apps.size()+" apps found",Toast.LENGTH_SHORT).show();
    }
    void exportApp(ApplicationInfo ai) {
        try {
            PackageInfo pi=getPackageManager().getPackageInfo(ai.packageName,0);
            ArrayList<String> paths=new ArrayList<>(); paths.add(pi.applicationInfo.sourceDir);
            if(pi.applicationInfo.splitSourceDirs!=null) paths.addAll(Arrays.asList(pi.applicationInfo.splitSourceDirs));
            String label=getPackageManager().getApplicationLabel(ai).toString().replaceAll("[^A-Za-z0-9._-]","_");
            File dir=new File(getExternalFilesDir(null),"exported_apks/"+label+"_"+ai.packageName);
            if(!dir.exists()&&!dir.mkdirs()) throw new IOException("Cannot create output folder");
            ArrayList<Uri> uris=new ArrayList<>();
            for(int i=0;i<paths.size();i++) {
                File out=new File(dir,i==0?"base.apk":"split_"+i+".apk");
                try(InputStream in=new FileInputStream(paths.get(i)); OutputStream os=new FileOutputStream(out)) { byte[] buf=new byte[32768]; int n; while((n=in.read(buf))>0) os.write(buf,0,n); }
                uris.add(Uri.parse("content://"+getPackageName()+".fileprovider/"+Uri.encode("exported_apks/"+label+"_"+ai.packageName+"/"+out.getName())));
            }
            Toast.makeText(this,"Saved "+uris.size()+" APK file(s) to app storage. Choose a destination in the share menu.",Toast.LENGTH_LONG).show();
            Intent share=new Intent(Intent.ACTION_SEND_MULTIPLE); share.setType("application/vnd.android.package-archive"); share.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris); share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Copy or share APK files"));
        } catch(Exception e) { new android.app.AlertDialog.Builder(this).setTitle("Export failed").setMessage(e.toString()).setPositiveButton("OK",null).show(); }
    }
}




