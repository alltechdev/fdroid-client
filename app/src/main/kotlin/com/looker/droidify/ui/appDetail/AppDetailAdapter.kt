package com.looker.droidify.ui.appDetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.util.TypedValueCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.looker.droidify.R
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.data.local.model.Reproducible
import com.looker.droidify.data.local.model.toReproducible
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.ProductPreference
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.model.findSuggested
import com.looker.droidify.network.DataSize
import com.looker.droidify.network.percentBy
import com.looker.droidify.utility.common.extension.authentication
import com.looker.droidify.utility.common.extension.copyToClipboard
import com.looker.droidify.utility.common.extension.corneredBackground
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.dpToPx
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.getDrawableCompat
import com.looker.droidify.utility.common.extension.getMutatedIcon
import com.looker.droidify.utility.common.extension.inflate
import com.looker.droidify.utility.common.extension.open
import com.looker.droidify.utility.common.extension.setTextSizeScaled
import com.looker.droidify.utility.common.nullIfEmpty
import com.looker.droidify.utility.common.sdkName
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.utility.text.formatHtml
import com.looker.droidify.widget.StableRecyclerAdapter
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.parcelize.Parcelize
import com.google.android.material.R as MaterialR
import com.looker.droidify.R.drawable as drawableRes
import com.looker.droidify.R.string as stringRes

@OptIn(ExperimentalTime::class)
class AppDetailAdapter(private val callbacks: Callbacks) :
    StableRecyclerAdapter<AppDetailAdapter.ViewType, RecyclerView.ViewHolder>() {

    interface Callbacks {
        fun onActionClick(action: Action)
        fun onPreferenceChanged(preference: ProductPreference)
        fun onScreenshotClick(position: Int)
        fun onRequestAddRepository(address: String)
        fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean
    }

    enum class Action(@param:StringRes val titleResId: Int, @param:DrawableRes val iconResId: Int) {
        INSTALL(stringRes.install, drawableRes.ic_download),
        UPDATE(stringRes.update, drawableRes.ic_download),
        LAUNCH(stringRes.launch, drawableRes.ic_launch),
        CANCEL(stringRes.cancel, drawableRes.ic_cancel),
    }

    sealed interface Status {
        data object Idle : Status
        data object Pending : Status
        data object Connecting : Status
        data class Downloading(val read: DataSize, val total: DataSize?) : Status
        data object PendingInstall : Status
        data object Installing : Status
    }

    enum class ViewType {
        APP_INFO,
        DOWNLOAD_STATUS,
        INSTALL_BUTTON,
        SCREENSHOT,
        SECTION,
        EXPAND,
        TEXT,
        LINK,
        EMPTY
    }

    private enum class SectionType(
        val titleResId: Int,
        val colorAttrResId: Int = android.R.attr.colorPrimary,
    ) {
        CHANGES(stringRes.changes),
        DONATE(stringRes.donate),
    }

    internal enum class ExpandType {
        NOTHING, DESCRIPTION, CHANGES,
        DONATES
    }

    private enum class TextType { DESCRIPTION, CHANGES }

    private sealed class Item {
        abstract val descriptor: String
        abstract val viewType: ViewType

        class AppInfoItem(
            val repository: Repository,
            val product: Product,
            val downloads: Long,
        ) : Item() {
            override val descriptor: String
                get() = "app_info.${product.name}"

            override val viewType: ViewType
                get() = ViewType.APP_INFO
        }

        data object DownloadStatusItem : Item() {
            override val descriptor: String
                get() = "download_status"
            override val viewType: ViewType
                get() = ViewType.DOWNLOAD_STATUS
        }

        data object InstallButtonItem : Item() {
            override val descriptor: String
                get() = "install_button"
            override val viewType: ViewType
                get() = ViewType.INSTALL_BUTTON
        }

        class ScreenshotItem(
            val screenshots: List<Product.Screenshot>,
            val packageName: String,
            val repository: Repository,
        ) : Item() {
            override val descriptor: String
                get() = "screenshot.${screenshots.size}"
            override val viewType: ViewType
                get() = ViewType.SCREENSHOT
        }

        class SectionItem(
            val sectionType: SectionType,
            val expandType: ExpandType,
            val items: List<Item>,
            val collapseCount: Int,
        ) : Item() {
            constructor(sectionType: SectionType) : this(
                sectionType,
                ExpandType.NOTHING,
                emptyList(),
                0
            )

            override val descriptor: String
                get() = "section.${sectionType.name}"

            override val viewType: ViewType
                get() = ViewType.SECTION
        }

        class ExpandItem(
            val expandType: ExpandType,
            val replace: Boolean,
            val items: List<Item>,
        ) : Item() {
            override val descriptor: String
                get() = "expand.${expandType.name}"

            override val viewType: ViewType
                get() = ViewType.EXPAND
        }

        class TextItem(val textType: TextType, val text: CharSequence) : Item() {
            override val descriptor: String
                get() = "text.${textType.name}"

            override val viewType: ViewType
                get() = ViewType.TEXT
        }

        sealed class LinkItem : Item() {
            override val viewType: ViewType
                get() = ViewType.LINK

            abstract val iconResId: Int
            abstract fun getTitle(context: Context): String
            abstract val uri: Uri?

            val displayLink: String?
                get() = uri?.schemeSpecificPart?.nullIfEmpty()
                    ?.let { if (it.startsWith("//")) null else it } ?: uri?.toString()

            class Donate(val donate: Product.Donate) : LinkItem() {
                override val descriptor: String
                    get() = "link.donate.$donate"

                override val iconResId: Int
                    get() = when (donate) {
                        is Product.Donate.Regular -> drawableRes.ic_donate
                        is Product.Donate.Bitcoin -> drawableRes.ic_donate_bitcoin
                        is Product.Donate.Litecoin -> drawableRes.ic_donate_litecoin
                        is Product.Donate.Liberapay -> drawableRes.ic_donate_liberapay
                        is Product.Donate.OpenCollective -> drawableRes.ic_donate_opencollective
                    }

                override fun getTitle(context: Context): String = when (donate) {
                    is Product.Donate.Regular -> context.getString(stringRes.website)
                    is Product.Donate.Bitcoin -> "Bitcoin"
                    is Product.Donate.Litecoin -> "Litecoin"
                    is Product.Donate.Liberapay -> "Liberapay"
                    is Product.Donate.OpenCollective -> "Open Collective"
                }

                override val uri: Uri = when (donate) {
                    is Product.Donate.Regular -> donate.url
                    is Product.Donate.Bitcoin -> "bitcoin:${donate.address}"
                    is Product.Donate.Litecoin -> "litecoin:${donate.address}"
                    is Product.Donate.Liberapay -> "https://liberapay.com/${donate.id}"
                    is Product.Donate.OpenCollective -> "https://opencollective.com/${donate.id}"
                }.toUri()
            }
        }

        class EmptyItem(val packageName: String, val repoAddress: String?) : Item() {
            override val descriptor: String
                get() = "empty"

            override val viewType: ViewType
                get() = ViewType.EMPTY
        }
    }

    private class Measurement<T : Any> {
        private var density = 0f
        private var scaledDensity = 0f
        private lateinit var metric: T

        fun measure(view: View) {
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                .let { view.measure(it, it) }
        }

        @Suppress("DEPRECATION")
        fun invalidate(resources: Resources, callback: () -> T): T {
            val metrics = resources.displayMetrics
            val density = metrics.density
            val scaledDensity = metrics.scaledDensity
            if (this.density != density || this.scaledDensity != scaledDensity) {
                this.density = density
                this.scaledDensity = scaledDensity
                metric = callback()
            }
            return metric
        }
    }

    private class AppInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.findViewById<ShapeableImageView>(R.id.app_icon)!!
        val name = itemView.findViewById<TextView>(R.id.app_name)!!
        val authorName = itemView.findViewById<TextView>(R.id.author_name)!!
        val packageName = itemView.findViewById<TextView>(R.id.package_name)!!
        val textSwitcher = itemView.findViewById<TextSwitcher>(R.id.author_package_name)!!

        init {
            textSwitcher.setInAnimation(itemView.context!!, R.anim.slide_right_fade_in)
            textSwitcher.setOutAnimation(itemView.context!!, R.anim.slide_right_fade_out)
        }

        val version = itemView.findViewById<TextView>(R.id.version)!!
        val size = itemView.findViewById<TextView>(R.id.size)!!
        val downloadsBlockDividier =
            itemView.findViewById<MaterialDivider>(R.id.downloads_block_divider)!!
        val downloadsBlock = itemView.findViewById<LinearLayout>(R.id.downloads_block)!!
        val downloads = itemView.findViewById<TextView>(R.id.downloads)!!
    }

    private class DownloadStatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val statusText = itemView.findViewById<TextView>(R.id.status)!!
        val progress = itemView.findViewById<LinearProgressIndicator>(R.id.progress)!!
    }

    private class InstallButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = itemView.findViewById<MaterialButton>(R.id.action)!!

        val actionTintNormal = button.context.getColorFromAttr(android.R.attr.colorPrimary)
        val actionTintOnNormal = button.context.getColorFromAttr(MaterialR.attr.colorOnPrimary)
        val actionTintCancel = button.context.getColorFromAttr(R.attr.colorError)
        val actionTintOnCancel = button.context.getColorFromAttr(MaterialR.attr.colorOnError)
        val actionTintDisabled = button.context.getColorFromAttr(MaterialR.attr.colorOutline)
        val actionTintOnDisabled = button.context.getColorFromAttr(android.R.attr.colorBackground)

        init {
            button.height = itemView.resources.sizeScaled(48)
        }
    }

    private class ScreenShotViewHolder(context: Context) :
        RecyclerView.ViewHolder(RecyclerView(context)) {

        val screenshotsRecycler: RecyclerView
            get() = itemView as RecyclerView
    }

    private class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.title)!!
        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
    }

    private class ExpandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = itemView.findViewById<MaterialButton>(R.id.expand_view_button)!!
    }

    private class TextViewHolder(context: Context) :
        RecyclerView.ViewHolder(TextView(context)) {
        val text: TextView
            get() = itemView as TextView

        init {
            with(itemView as TextView) {
                setTextIsSelectable(true)
                setTextSizeScaled(15)
                16.dp.let { itemView.setPadding(it, it, it, it) }
                movementMethod = LinkMovementMethod()
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private open class OverlappingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            // Block touch events if touched above negative margin
            itemView.setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_DOWN && run {
                    val top = (itemView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
                    top < 0 && event.y < -top
                }
            }
        }
    }

    private class LinkViewHolder(itemView: View) : OverlappingViewHolder(itemView) {
        companion object {
            private val measurement = Measurement<Int>()
        }

        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
        val text = itemView.findViewById<TextView>(R.id.text)!!
        val link = itemView.findViewById<TextView>(R.id.link)!!

        init {
            text.typeface = TypefaceExtra.medium
            val margin = measurement.invalidate(itemView.resources) {
                @SuppressLint("SetTextI18n")
                text.text = "measure"
                link.visibility = View.GONE
                measurement.measure(itemView)
                ((itemView.measuredHeight - icon.measuredHeight) / 2f).roundToInt()
            }
            (icon.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin += margin
                bottomMargin += margin
            }
        }
    }

    private class EmptyViewHolder(context: Context) :
        RecyclerView.ViewHolder(LinearLayout(context)) {
        val packageName = TextView(context)
        val repoTitle = TextView(context)
        val repoAddress = TextView(context)
        val copyRepoAddress = MaterialButton(context)

        init {
            with(itemView as LinearLayout) {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(20.dp, 20.dp, 20.dp, 20.dp)
                val imageView = ImageView(context)
                val bitmap = createBitmap(64.dp.dpToPx.roundToInt(), 32.dp.dpToPx.roundToInt())
                val canvas = Canvas(bitmap)
                val title = TextView(context)
                with(title) {
                    gravity = Gravity.CENTER
                    typeface = TypefaceExtra.medium
                    setTextColor(context.getColorFromAttr(android.R.attr.colorPrimary))
                    setTextSizeScaled(20)
                    setText(stringRes.application_not_found)
                    setPadding(0, 12.dp, 0, 12.dp)
                }
                with(packageName) {
                    gravity = Gravity.CENTER
                    setTextColor(context.getColorFromAttr(MaterialR.attr.colorOutline))
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSizeScaled(16)
                    background = context.corneredBackground
                    setPadding(0, 12.dp, 0, 12.dp)
                }
                val waveHeight = 2.dp.dpToPx
                val waveWidth = 12.dp.dpToPx
                with(canvas) {
                    val linePaint = Paint().apply {
                        color = context.getColorFromAttr(MaterialR.attr.colorOutline).defaultColor
                        strokeWidth = 8f
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    for (x in 12..(width - 12)) {
                        val yValue =
                            (
                                    (
                                            sin(x * (2f * PI / waveWidth)) *
                                                    (waveHeight / (2)) +
                                                    (waveHeight / 2)
                                            ).toFloat() +
                                            (0 - (waveHeight / 2))
                                    ) + height / 2
                        drawPoint(x.toFloat(), yValue, linePaint)
                    }
                }
                imageView.load(bitmap)
                with(repoTitle) {
                    gravity = Gravity.CENTER
                    typeface = TypefaceExtra.medium
                    setTextColor(context.getColorFromAttr(android.R.attr.colorPrimary))
                    setTextSizeScaled(20)
                    setPadding(0, 0, 0, 12.dp)
                }
                with(repoAddress) {
                    gravity = Gravity.CENTER
                    setTextColor(context.getColorFromAttr(MaterialR.attr.colorOutline))
                    typeface = Typeface.DEFAULT_BOLD
                    setTextSizeScaled(16)
                    background = context.corneredBackground
                    setPadding(0, 12.dp, 0, 12.dp)
                }
                with(copyRepoAddress) {
                    icon = context.open
                    setText(stringRes.add_repository)
                    setBackgroundColor(context.getColor(android.R.color.transparent))
                    setTextColor(context.getColorFromAttr(android.R.attr.colorPrimary))
                    iconTint = context.getColorFromAttr(android.R.attr.colorPrimary)
                }
                addView(
                    title,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(
                    packageName,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(
                    imageView,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(
                    repoTitle,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(
                    repoAddress,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(
                    copyRepoAddress,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    private val items = mutableListOf<Item>()
    private val expanded = mutableSetOf<ExpandType>()
    private var product: Product? = null
    private var installedItem: InstalledItem? = null
    fun setProducts(
        context: Context,
        packageName: String,
        suggestedRepo: String? = null,
        products: List<Pair<Product, Repository>>,
        rblogs: List<RBLogEntity>,
        downloads: Long,
        installedItem: InstalledItem?,
    ) {
        items.clear()
        val productRepository = products.findSuggested(installedItem) ?: run {
            items += Item.EmptyItem(packageName, suggestedRepo)
            notifyDataSetChanged()
            return
        }

        this.product = productRepository.first
        this.installedItem = installedItem

        items += Item.AppInfoItem(
            productRepository.second,
            productRepository.first,
            downloads
        )

        items += Item.DownloadStatusItem
        items += Item.InstallButtonItem

        if (productRepository.first.screenshots.isNotEmpty()) {
            val screenShotItem = mutableListOf<Item>()
            screenShotItem += Item.ScreenshotItem(
                productRepository.first.screenshots,
                packageName,
                productRepository.second
            )
            items += screenShotItem
        }

        val textViewHolder = TextViewHolder(context)
        val textViewWidthSpec = context.resources.displayMetrics.widthPixels
            .let { View.MeasureSpec.makeMeasureSpec(it, View.MeasureSpec.EXACTLY) }
        val textViewHeightSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        fun CharSequence.lineCropped(maxLines: Int, cropLines: Int): CharSequence? {
            assert(cropLines <= maxLines)
            textViewHolder.text.text = this
            textViewHolder.text.measure(textViewWidthSpec, textViewHeightSpec)
            textViewHolder.text.layout(
                0,
                0,
                textViewHolder.text.measuredWidth,
                textViewHolder.text.measuredHeight
            )
            val layout = textViewHolder.text.layout
            val cropLineOffset =
                if (layout.lineCount <= maxLines) -1 else layout.getLineEnd(cropLines - 1)
            val paragraphEndIndex = if (cropLineOffset < 0) {
                -1
            } else {
                indexOf("\n\n", cropLineOffset).let { if (it >= 0) it else length }
            }
            val paragraphEndLine = if (paragraphEndIndex < 0) {
                -1
            } else {
                layout.getLineForOffset(paragraphEndIndex).apply { assert(this >= 0) }
            }
            val end = when {
                cropLineOffset < 0 -> -1
                paragraphEndLine >= 0 && paragraphEndLine - (cropLines - 1) <= 3 ->
                    if (paragraphEndIndex < length) paragraphEndIndex else -1

                else -> cropLineOffset
            }
            val length = if (end < 0) {
                -1
            } else {
                asSequence().take(end)
                    .indexOfLast { it != '\n' }.let { if (it >= 0) it + 1 else end }
            }
            return if (length >= 0) subSequence(0, length) else null
        }

        val description = formatHtml(productRepository.first.description) { url ->
            val uri = try {
                url.toUri()
            } catch (_: Exception) {
                null
            }
            if (uri != null) {
                callbacks.onUriClick(uri, true)
            }
        }.apply {
            if (productRepository.first.let { it.summary.isNotEmpty() && it.name != it.summary }) {
                if (isNotEmpty()) {
                    insert(0, "\n\n")
                }
                insert(0, productRepository.first.summary)
                if (isNotEmpty()) {
                    setSpan(
                        TypefaceSpan("sans-serif-medium"),
                        0,
                        productRepository.first.summary.length,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        if (description.isNotEmpty()) {
            val cropped = if (ExpandType.DESCRIPTION !in expanded) {
                description.lineCropped(
                    12,
                    10
                )
            } else {
                null
            }
            val item = Item.TextItem(TextType.DESCRIPTION, description)
            if (cropped != null) {
                val croppedItem = Item.TextItem(TextType.DESCRIPTION, cropped)
                items += listOf(
                    croppedItem,
                    Item.ExpandItem(ExpandType.DESCRIPTION, true, listOf(item, croppedItem))
                )
            } else {
                items += item
            }
        }

        val changes = productRepository.first.whatsNew
        if (changes.isNotEmpty()) {
            items += Item.SectionItem(SectionType.CHANGES)
            val cropped =
                if (ExpandType.CHANGES !in expanded) {
                    changes.lineCropped(12, 10)
                } else {
                    null
                }
            val item = Item.TextItem(TextType.CHANGES, changes)
            if (cropped != null) {
                val croppedItem = Item.TextItem(TextType.CHANGES, cropped)
                items += listOf(
                    croppedItem,
                    Item.ExpandItem(ExpandType.CHANGES, true, listOf(item, croppedItem))
                )
            } else {
                items += item
            }
        }

        val donateItems = productRepository.first.donates.map(Item.LinkItem::Donate)
        if (donateItems.isNotEmpty()) {
            if (ExpandType.DONATES in expanded) {
                items += Item.SectionItem(
                    SectionType.DONATE,
                    ExpandType.DONATES,
                    emptyList(),
                    donateItems.size
                )
                items += donateItems
            } else {
                items += Item.SectionItem(
                    SectionType.DONATE,
                    ExpandType.DONATES,
                    donateItems,
                    0
                )
            }
        }

        this.product = productRepository.first
        this.installedItem = installedItem
        notifyDataSetChanged()
    }

    var action: Action? = null
        set(value) {
            val index = items.indexOf(Item.InstallButtonItem)
            val progressBarIndex = items.indexOf(Item.DownloadStatusItem)
            if (index > 0 && progressBarIndex > 0) {
                notifyItemChanged(index)
                notifyItemChanged(progressBarIndex)
            }
            field = value
        }

    var status: Status = Status.Idle
        set(value) {
            if (field != value) {
                val index = items.indexOf(Item.DownloadStatusItem)
                if (index > 0) notifyItemChanged(index)
            }
            field = value
        }

    override val viewTypeClass: Class<ViewType>
        get() = ViewType::class.java

    override fun getItemCount(): Int = items.size
    override fun getItemDescriptor(position: Int): String = items[position].descriptor
    override fun getItemEnumViewType(position: Int): ViewType = items[position].viewType

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: ViewType,
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.APP_INFO -> AppInfoViewHolder(parent.inflate(R.layout.app_detail_header))

            ViewType.DOWNLOAD_STATUS -> DownloadStatusViewHolder(
                parent.inflate(R.layout.download_status)
            )

            ViewType.INSTALL_BUTTON -> InstallButtonViewHolder(
                parent.inflate(R.layout.install_button)
            ).apply {
                button.setOnClickListener { action?.let(callbacks::onActionClick) }
            }

            ViewType.SCREENSHOT -> ScreenShotViewHolder(parent.context)

            ViewType.SECTION -> SectionViewHolder(parent.inflate(R.layout.section_item)).apply {
                itemView.setOnClickListener {
                    val position = absoluteAdapterPosition
                    val sectionItem = items[position] as Item.SectionItem
                    if (sectionItem.items.isNotEmpty()) {
                        expanded += sectionItem.expandType
                        items[position] = Item.SectionItem(
                            sectionItem.sectionType,
                            sectionItem.expandType,
                            emptyList(),
                            sectionItem.items.size + sectionItem.collapseCount
                        )
                        notifyItemChanged(position)
                        items.addAll(position + 1, sectionItem.items)
                        notifyItemRangeInserted(position + 1, sectionItem.items.size)
                    } else if (sectionItem.collapseCount > 0) {
                        expanded -= sectionItem.expandType
                        items[position] = Item.SectionItem(
                            sectionItem.sectionType,
                            sectionItem.expandType,
                            items.subList(position + 1, position + 1 + sectionItem.collapseCount)
                                .toList(),
                            0
                        )
                        notifyItemChanged(position)
                        repeat(sectionItem.collapseCount) { items.removeAt(position + 1) }
                        notifyItemRangeRemoved(position + 1, sectionItem.collapseCount)
                    }
                }
            }

            ViewType.EXPAND -> ExpandViewHolder(parent.inflate(R.layout.expand_view_button))
                .apply {
                    itemView.setOnClickListener {
                        val position = absoluteAdapterPosition
                        val expandItem = items[position] as Item.ExpandItem
                        if (expandItem.expandType !in expanded) {
                            expanded += expandItem.expandType
                            if (expandItem.replace) {
                                items[position - 1] = expandItem.items[0]
                                notifyItemRangeChanged(position - 1, 2)
                            } else {
                                items.addAll(position, expandItem.items)
                                if (position > 0) {
                                    notifyItemRangeInserted(position, expandItem.items.size)
                                    notifyItemChanged(position + expandItem.items.size)
                                }
                            }
                        } else {
                            expanded -= expandItem.expandType
                            if (expandItem.replace) {
                                items[position - 1] = expandItem.items[1]
                                notifyItemRangeChanged(position - 1, 2)
                            } else {
                                items.removeAll(expandItem.items)
                                if (position > 0) {
                                    notifyItemRangeRemoved(
                                        position - expandItem.items.size,
                                        expandItem.items.size
                                    )
                                    notifyItemChanged(position - expandItem.items.size)
                                }
                            }
                        }
                    }
                }

            ViewType.TEXT -> TextViewHolder(parent.context)
            ViewType.LINK -> LinkViewHolder(parent.inflate(R.layout.link_item)).apply {
                itemView.setOnClickListener {
                    val linkItem = items[absoluteAdapterPosition] as Item.LinkItem
                    if (linkItem.uri?.let { callbacks.onUriClick(it, false) } != true) {
                        linkItem.displayLink?.let { copyLinkToClipboard(itemView, it) }
                    }
                }
                itemView.setOnLongClickListener {
                    val linkItem = items[absoluteAdapterPosition] as Item.LinkItem
                    linkItem.displayLink?.let { copyLinkToClipboard(itemView, it) }
                    true
                }
            }

            ViewType.EMPTY -> EmptyViewHolder(parent.context).apply {
                copyRepoAddress.setOnClickListener {
                    repoAddress.text?.let { link ->
                        callbacks.onRequestAddRepository(link.toString())
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>,
    ) {
        val context = holder.itemView.context
        val item = items[position]
        when (getItemEnumViewType(position)) {
            ViewType.APP_INFO -> {
                holder as AppInfoViewHolder
                item as Item.AppInfoItem
                var showAuthor = item.product.author.name.isNotEmpty()
                val iconUrl =
                    item.product.item().icon(view = holder.icon, repository = item.repository)
                holder.icon.load(iconUrl) {
                    authentication(item.repository.authentication)
                }
                val authorText =
                    if (showAuthor) {
                        buildSpannedString {
                            append("by ")
                            bold { append(item.product.author.name) }
                        }
                    } else {
                        buildSpannedString { bold { append(item.product.packageName) } }
                    }
                holder.authorName.text = authorText
                holder.packageName.text = authorText
                if (item.product.author.name.isNotEmpty()) {
                    holder.icon.setOnClickListener {
                        showAuthor = !showAuthor
                        val newText = if (showAuthor) {
                            buildSpannedString {
                                append("by ")
                                bold { append(item.product.author.name) }
                            }
                        } else {
                            buildSpannedString { bold { append(item.product.packageName) } }
                        }
                        holder.textSwitcher.setText(newText)
                    }
                }
                holder.name.text = item.product.name

                holder.version.apply {
                    text = installedItem?.version ?: product?.version
                    if (product?.canUpdate(installedItem) == true) {
                        if (background == null) {
                            background = context.corneredBackground
                            setPadding(8.dp, 4.dp, 8.dp, 4.dp)
                            backgroundTintList =
                                context.getColorFromAttr(MaterialR.attr.colorTertiaryContainer)
                            setTextColor(context.getColorFromAttr(MaterialR.attr.colorOnTertiaryContainer))
                        }
                    } else {
                        if (background != null) {
                            setPadding(0, 0, 0, 0)
                            setTextColor(
                                context.getColorFromAttr(android.R.attr.colorControlNormal)
                            )
                            background = null
                        }
                    }
                }
                holder.size.text = DataSize(product?.displayRelease?.size ?: 0).toString()

                holder.downloadsBlockDividier.isGone = item.downloads < 1
                holder.downloadsBlock.isGone = item.downloads < 1
                holder.downloads.text = item.downloads.toString()
            }

            ViewType.DOWNLOAD_STATUS -> {
                holder as DownloadStatusViewHolder
                item as Item.DownloadStatusItem
                val status = status
                holder.itemView.isVisible = status != Status.Idle
                holder.statusText.isVisible = status != Status.Idle
                holder.progress.isVisible = status != Status.Idle
                if (status != Status.Idle) {
                    when (status) {
                        is Status.Pending -> {
                            holder.statusText.setText(stringRes.waiting_to_start_download)
                            holder.progress.isIndeterminate = true
                        }

                        is Status.Connecting -> {
                            holder.statusText.setText(stringRes.connecting)
                            holder.progress.isIndeterminate = true
                        }

                        is Status.Downloading -> {
                            holder.statusText.text = context.getString(
                                stringRes.downloading_FORMAT,
                                if (status.total == null) {
                                    status.read.toString()
                                } else {
                                    "${status.read} / ${status.total}"
                                }
                            )
                            holder.progress.isIndeterminate = status.total == null
                            if (status.total != null) {
                                holder.progress.setProgressCompat(
                                    status.read.value percentBy status.total.value,
                                    true
                                )
                            }
                        }

                        Status.Installing -> {
                            holder.statusText.setText(stringRes.installing)
                            holder.progress.isIndeterminate = true
                        }

                        Status.PendingInstall -> {
                            holder.statusText.setText(stringRes.waiting_to_start_installation)
                            holder.progress.isIndeterminate = true
                        }

                        Status.Idle -> {}
                    }
                }
            }

            ViewType.INSTALL_BUTTON -> {
                holder as InstallButtonViewHolder
                item as Item.InstallButtonItem
                val action = action
                holder.button.apply {
                    isEnabled = action != null
                    if (action != null) {
                        icon = context.getDrawableCompat(action.iconResId)
                        setText(action.titleResId)
                        setTextColor(
                            if (action == Action.CANCEL) {
                                holder.actionTintOnCancel
                            } else {
                                holder.actionTintOnNormal
                            }
                        )
                        backgroundTintList = if (action == Action.CANCEL) {
                            holder.actionTintCancel
                        } else {
                            holder.actionTintNormal
                        }
                        iconTint = if (action == Action.CANCEL) {
                            holder.actionTintOnCancel
                        } else {
                            holder.actionTintOnNormal
                        }
                    } else {
                        icon = context.getDrawableCompat(drawableRes.ic_cancel)
                        setText(stringRes.cancel)
                        setTextColor(holder.actionTintOnDisabled)
                        backgroundTintList = holder.actionTintDisabled
                        iconTint = holder.actionTintOnDisabled
                    }
                }
            }

            ViewType.SCREENSHOT -> {
                holder as ScreenShotViewHolder
                item as Item.ScreenshotItem
                holder.screenshotsRecycler.run {
                    if (layoutManager == null) {
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = false
                        clipToPadding = false
                        val padding = 8.dp
                        setPadding(padding, padding, padding, padding)
                        layoutManager =
                            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    }
                    val screenshotsAdapter = (adapter as? ScreenshotsAdapter)
                        ?: ScreenshotsAdapter(callbacks::onScreenshotClick).also { adapter = it }
                    screenshotsAdapter.setScreenshots(
                        item.repository,
                        item.packageName,
                        item.screenshots
                    )
                }
            }

            ViewType.SECTION -> {
                holder as SectionViewHolder
                item as Item.SectionItem

                val expandable = item.items.isNotEmpty() || item.collapseCount > 0
                holder.itemView.isEnabled = expandable
                holder.itemView.let {
                    it.setPadding(
                        it.paddingLeft,
                        it.paddingTop,
                        it.paddingRight,
                        if (expandable) it.paddingTop else 0
                    )
                }
                val color = context.getColorFromAttr(item.sectionType.colorAttrResId)
                holder.title.setTextColor(color)
                holder.title.text = context.getString(item.sectionType.titleResId)
                holder.icon.isVisible = expandable
                holder.icon.scaleY = if (item.collapseCount > 0) -1f else 1f
                holder.icon.imageTintList = color
            }

            ViewType.EXPAND -> {
                holder as ExpandViewHolder
                item as Item.ExpandItem
                holder.button.text = if (item.expandType !in expanded) {
                    context.getString(stringRes.show_more)
                } else {
                    context.getString(stringRes.show_less)
                }
            }

            ViewType.TEXT -> {
                holder as TextViewHolder
                item as Item.TextItem
                holder.text.text = item.text
            }

            ViewType.LINK -> {
                holder as LinkViewHolder
                item as Item.LinkItem
                val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin =
                    if (position > 0 && items[position - 1] !is Item.LinkItem) {
                        -context.resources.sizeScaled(8)
                    } else {
                        0
                    }
                holder.itemView.isEnabled = item.uri != null
                holder.icon.setImageResource(item.iconResId)
                holder.text.text = item.getTitle(context)
                holder.link.isVisible = item.uri != null
                holder.link.text = item.displayLink
            }

            ViewType.EMPTY -> {
                holder as EmptyViewHolder
                item as Item.EmptyItem
                holder.packageName.text = item.packageName
                if (item.repoAddress != null) {
                    holder.repoTitle.setText(stringRes.repository_not_found)
                    holder.repoAddress.text = item.repoAddress
                }
            }
        }
    }

    private fun copyLinkToClipboard(
        view: View,
        link: String,
        snackbarText: Int = stringRes.link_copied_to_clipboard
    ) {
        view.context.copyToClipboard(link)
        Snackbar.make(view, snackbarText, Snackbar.LENGTH_SHORT).show()
    }

    @Parcelize
    class SavedState internal constructor(internal val expanded: Set<ExpandType>) : Parcelable

    fun saveState(): SavedState? {
        return if (expanded.isNotEmpty()) {
            SavedState(expanded)
        } else {
            null
        }
    }

    fun restoreState(savedState: SavedState) {
        expanded.clear()
        expanded += savedState.expanded
    }
}
