package com.example.apkcopy;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.Deflater;

public class MainActivity extends Activity {
    LinearLayout list;
    ArrayList<ApplicationInfo> apps = new ArrayList<>();
    File pendingExport;
    volatile boolean isExporting;
    static final int SAVE_EXPORT_REQUEST = 42;
    int blue = 0xff1769aa;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(24,24,24,16); root.setBackgroundColor(0xfff5f7fa);
        TextView title = new TextView(this); title.setText("APK Copier"); title.setTextSize(26); title.setTypeface(null, Typeface.BOLD); title.setTextColor(0xff17212b); root.addView(title);
        TextView desc = new TextView(this); desc.setText("Export installed app APK files as a ZIP you can share or save to your device."); desc.setTextSize(15); desc.setTextColor(0xff52606d); desc.setPadding(0,8,0,14); root.addView(desc);
        Button refresh = new Button(this); refresh.setText("Refresh apps"); refresh.setOnClickListener(v -> loadApps()); root.addView(refresh);
        ScrollView sc = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(1); sc.addView(list); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); loadApps();
    }
    void loadApps() {
        list.removeAllViews(); apps.clear(); PackageManager pm=getPackageManager();
        List<ApplicationInfo> all=pm.getInstalledApplications(0);
        Collections.sort(all,(a,b)->{
            boolean aPinned="com.playgilla.gilla".equals(a.packageName);
            boolean bPinned="com.playgilla.gilla".equals(b.packageName);
            if(aPinned!=bPinned) return aPinned?-1:1;
            return pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString());
        });
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
        if(isExporting) { Toast.makeText(this,"An export is already being prepared",Toast.LENGTH_SHORT).show(); return; }
        isExporting=true;
        try {
            PackageInfo pi=getPackageManager().getPackageInfo(ai.packageName,0);
            ArrayList<String> paths=new ArrayList<>(); paths.add(pi.applicationInfo.sourceDir);
            if(pi.applicationInfo.splitSourceDirs!=null) paths.addAll(Arrays.asList(pi.applicationInfo.splitSourceDirs));
            String label=getPackageManager().getApplicationLabel(ai).toString().replaceAll("[^A-Za-z0-9._-]","_");
            File externalDir=getExternalFilesDir(null);
            if(externalDir==null) throw new IOException("App storage is unavailable");
            File dir=new File(externalDir,"exported_apks");
            if(!dir.exists()&&!dir.mkdirs()) throw new IOException("Cannot create output folder");
            File archive=new File(dir,label+"_"+ai.packageName+"_APKs.zip");
            Toast.makeText(this,"Preparing APK export…",Toast.LENGTH_SHORT).show();
            new Thread(()->{
                try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))) {
                    zip.setLevel(Deflater.NO_COMPRESSION);
                    byte[] buf=new byte[65536];
                    for(int i=0;i<paths.size();i++) {
                        zip.putNextEntry(new ZipEntry(i==0?"base.apk":"split_"+i+".apk"));
                        try(InputStream in=new FileInputStream(paths.get(i))) { int n; while((n=in.read(buf))>0) zip.write(buf,0,n); }
                        zip.closeEntry();
                    }
                    runOnUiThread(()->showExportReady(archive,paths.size()));
                } catch(Exception e) {
                    runOnUiThread(()->showExportError(e));
                } finally { isExporting=false; }
            },"apk-export-worker").start();
        } catch(Exception e) { isExporting=false; showExportError(e); }
    }

    void showExportReady(File archive,int apkCount) {
        pendingExport=archive;
        new android.app.AlertDialog.Builder(this)
            .setTitle("APK export ready")
            .setMessage("The export contains "+apkCount+" APK file(s). Share it or save a copy to a folder on your device.")
            .setPositiveButton("Share",(d,w)->shareExport(archive))
            .setNeutralButton("Save copy to device",(d,w)->saveExport(archive))
            .setNegativeButton("Cancel",null)
            .show();
    }

    void showExportError(Exception e) {
        new android.app.AlertDialog.Builder(this).setTitle("Export failed").setMessage(e.toString()).setPositiveButton("OK",null).show();
    }

    void shareExport(File archive) {
        Uri uri=Uri.parse("content://"+getPackageName()+".fileprovider/"+Uri.encode("exported_apks/"+archive.getName()));
        Intent share=new Intent(Intent.ACTION_SEND); share.setType("application/zip"); share.putExtra(Intent.EXTRA_STREAM,uri);
        share.setClipData(ClipData.newUri(getContentResolver(),"APK export",uri)); share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share,"Copy or share APK export"));
    }

    void saveExport(File archive) {
        try {
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE,archive.getName());
            startActivityForResult(intent,SAVE_EXPORT_REQUEST);
        } catch(Exception e) {
            new android.app.AlertDialog.Builder(this).setTitle("Save picker unavailable")
                .setMessage("Your headset does not provide a save picker. You can still share the export.")
                .setPositiveButton("Share instead",(d,w)->shareExport(archive)).setNegativeButton("Cancel",null).show();
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=SAVE_EXPORT_REQUEST||resultCode!=RESULT_OK||data==null||data.getData()==null||pendingExport==null) return;
        try(InputStream in=new FileInputStream(pendingExport); OutputStream out=getContentResolver().openOutputStream(data.getData())) {
            if(out==null) throw new IOException("Could not open the selected destination");
            byte[] buf=new byte[32768]; int n; while((n=in.read(buf))>0) out.write(buf,0,n);
            Toast.makeText(this,"Export copied to the selected folder",Toast.LENGTH_LONG).show();
        } catch(Exception e) { new android.app.AlertDialog.Builder(this).setTitle("Copy failed").setMessage(e.toString()).setPositiveButton("OK",null).show(); }
    }
}






