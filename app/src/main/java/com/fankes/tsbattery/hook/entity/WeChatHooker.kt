/*
 * TSBattery - A new way to save your battery avoid cancer apps hacker it.
 * Copyright (C) 2019-2022 Fankes Studio(qzmmcn@163.com)
 * https://github.com/fankes/TSBattery
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is Created by fankes on 2022/9/29.
 */
package com.fankes.tsbattery.hook.entity

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.fankes.tsbattery.R
import com.fankes.tsbattery.const.PackageName
import com.fankes.tsbattery.data.ConfigData
import com.fankes.tsbattery.hook.factory.hookSystemWakeLock
import com.fankes.tsbattery.hook.factory.jumpToModuleSettings
import com.fankes.tsbattery.hook.factory.startModuleSettings
import com.fankes.tsbattery.utils.factory.absoluteStatusBarHeight
import com.fankes.tsbattery.utils.factory.dp
import com.fankes.tsbattery.utils.factory.appVersionCode
import com.fankes.tsbattery.utils.factory.appVersionName
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.injectModuleAppResources
import com.highcapable.yukihookapi.hook.factory.processName
import com.highcapable.yukihookapi.hook.factory.registerModuleAppActivities
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.type.android.BundleClass

/**
 * Hook ??????
 *
 * ????????????????????????
 */
object WeChatHooker : YukiBaseHooker() {

    /** ?????????????????? - ???????????????????????????????????? */
    const val LauncherUIClass = "${PackageName.WECHAT}.ui.LauncherUI"

    /** ?????????????????? - ???????????????????????????????????? */
    private const val EmptyActivityClass = "${PackageName.WECHAT}.ui.EmptyActivity"

    /** ?????????????????? - ???????????????????????????????????? */
    private const val WelabMainUIClass = "${PackageName.WECHAT}.plugin.welab.ui.WelabMainUI"

    /** ?????????????????? - ???????????????????????????????????? */
    private const val SettingsUIClass = "${PackageName.WECHAT}.plugin.setting.ui.setting.SettingsUI"

    override fun onHook() {
        onAppLifecycle {
            onCreate {
                ConfigData.init(context = this)
                registerModuleAppActivities(
                    when {
                        EmptyActivityClass.hasClass() -> EmptyActivityClass
                        WelabMainUIClass.hasClass() -> WelabMainUIClass
                        else -> error("Inject WeChat Activity Proxy failed, unsupport version $appVersionName($appVersionCode)")
                    }
                )
                if (ConfigData.isDisableAllHook) return@onCreate
                hookSystemWakeLock()
                loggerI(msg = "All processes are completed for \"${processName.takeIf { it != packageName } ?: packageName}\"")
            }
        }
        /** ?????????????????? */
        withProcess(mainProcessName) {
            /** Hook ???????????? */
            LauncherUIClass.hook {
                injectMember {
                    method {
                        name = "onResume"
                        emptyParam()
                    }
                    afterHook { instance<Activity>().jumpToModuleSettings(isFinish = false) }
                }
            }
            /** ???????????????????????????????????? */
            SettingsUIClass.hook {
                injectMember {
                    method {
                        name = "onCreate"
                        param(BundleClass)
                    }
                    afterHook {
                        method {
                            name = "get_fragment"
                            emptyParam()
                            superClass(isOnlySuperClass = true)
                        }.get(instance).call()?.current()
                            ?.field { name = "mController" }
                            ?.current()?.method { name = "getContentView" }
                            ?.invoke<ViewGroup>()?.addView(LinearLayout(instance()).apply {
                                context.injectModuleAppResources()
                                gravity = Gravity.END or Gravity.BOTTOM
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                addView(ImageView(context).apply {
                                    layoutParams = ViewGroup.MarginLayoutParams(20.dp(context), 20.dp(context)).apply {
                                        topMargin = context.absoluteStatusBarHeight + 15.dp(context)
                                        rightMargin = 20.dp(context)
                                    }
                                    setColorFilter(ResourcesCompat.getColor(resources, R.color.colorTextGray, null))
                                    setImageResource(R.drawable.ic_icon)
                                    if (Build.VERSION.SDK_INT >= 26) tooltipText = "TSBattery ??????"
                                    setOnClickListener { context.startModuleSettings() }
                                })
                            })
                    }
                }
            }
        }
    }
}