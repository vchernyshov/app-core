package com.nullgr.androidcore.interactor.presentation

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.jakewharton.rxrelay2.BehaviorRelay
import com.nullgr.androidcore.R
import com.nullgr.androidcore.interactor.data.UserDataRepositoryExample
import com.nullgr.androidcore.interactor.domain.interactor.GetSuggestedFriendsUseCase
import com.nullgr.androidcore.interactor.domain.interactor.GetUserFriendsPhotoUseCase
import com.nullgr.androidcore.interactor.domain.interactor.GetUserPhoneUseCase
import com.nullgr.androidcore.interactor.domain.interactor.GetUserPhotoUseCase
import com.nullgr.androidcore.interactor.domain.interactor.GetUserUseCase
import com.nullgr.androidcore.interactor.domain.interactor.GetUsersUseCase
import com.nullgr.androidcore.interactor.domain.interactor.ResetPasswordUseCase
import com.nullgr.androidcore.interactor.domain.repository.UserRepositoryExample
import com.nullgr.androidcore.interactor.presentation.adapter.InteractorDelegatesFactory
import com.nullgr.androidcore.interactor.presentation.adapter.items.InteractorItem
import com.nullgr.corelibrary.adapter.AdapterDelegatesFactory
import com.nullgr.corelibrary.adapter.DiffCalculator
import com.nullgr.corelibrary.adapter.DynamicAdapter
import com.nullgr.corelibrary.adapter.RxDiffCalculator
import com.nullgr.corelibrary.adapter.items.ListItem
import com.nullgr.corelibrary.rx.RxBus
import com.nullgr.corelibrary.rx.asObservable
import com.nullgr.corelibrary.rx.bindProgress
import com.nullgr.corelibrary.rx.schedulers.ComputationToMainSchedulersFacade
import com.nullgr.corelibrary.rx.schedulers.SchedulersFacade
import com.nullgr.corelibrary.widgets.decor.DividerItemDecoration
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_interactor_example.*

/**
 * @author chernyshov.
 */
class InteractorExampleActivity
    : AppCompatActivity() {

    private val schedulersFacade: SchedulersFacade
    private val diffCalculator: DiffCalculator
    private val bus: RxBus
    private val delegatesFactory: AdapterDelegatesFactory
    private val adapter: DynamicAdapter
    private val compositeDisposable: CompositeDisposable

    private val repository: UserRepositoryExample
    private val getSuggestedFriendsUseCase: GetSuggestedFriendsUseCase
    private val getUserFriendsPhotoUseCase: GetUserFriendsPhotoUseCase
    private val getUserPhoneUseCase: GetUserPhoneUseCase
    private val getUserPhotoUseCase: GetUserPhotoUseCase
    private val getUsersUseCase: GetUsersUseCase
    private val getUserUseCase: GetUserUseCase
    private val resetPasswordUseCase: ResetPasswordUseCase

    private val progressState: BehaviorRelay<Boolean>
    private val progressView by lazy {
        ProgressDialog(this)
                .apply {
                    setMessage("Use case executing...")
                    setProgressStyle(ProgressDialog.STYLE_SPINNER)
                    setCanceledOnTouchOutside(false)
                    isIndeterminate = true
                }
    }

    init {
        schedulersFacade = ComputationToMainSchedulersFacade()
        diffCalculator = RxDiffCalculator(schedulersFacade)
        bus = RxBus()
        delegatesFactory = InteractorDelegatesFactory(bus)
        adapter = DynamicAdapter(delegatesFactory, diffCalculator)
        compositeDisposable = CompositeDisposable()

        repository = UserDataRepositoryExample()
        getSuggestedFriendsUseCase = GetSuggestedFriendsUseCase(repository, schedulersFacade)
        getUserFriendsPhotoUseCase = GetUserFriendsPhotoUseCase(repository, schedulersFacade)
        getUserPhoneUseCase = GetUserPhoneUseCase(repository, schedulersFacade)
        getUserPhotoUseCase = GetUserPhotoUseCase(repository, schedulersFacade)
        getUsersUseCase = GetUsersUseCase(repository, schedulersFacade)
        getUserUseCase = GetUserUseCase(repository, schedulersFacade)
        resetPasswordUseCase = ResetPasswordUseCase(repository, schedulersFacade)
        progressState = BehaviorRelay.createDefault(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interactor_example)

        itemsView.layoutManager = LinearLayoutManager(this)
        itemsView.addItemDecoration(DividerItemDecoration(this, R.drawable.divider_adapter_item))
        itemsView.adapter = adapter

        adapter.updateData(prepareItems())

        progressState.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    when {
                        it -> progressView.show()
                        else -> progressView.hide()
                    }
                }
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.OBSERVABLE)
                .skipWhileInProgress(progressState)
                .flatMap {
                    getUserPhotoUseCase.execute(EXAMPLE_ID)
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext { toast(it) }
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.OBSERVABLE_LIST)
                .skipWhileInProgress(progressState)
                .flatMap {
                    getUserFriendsPhotoUseCase.execute(EXAMPLE_ID)
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext { toast(it.toString()) }
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.FLOWABLE)
                .skipWhileInProgress(progressState)
                .flatMap {
                    getUserUseCase.execute(EXAMPLE_ID)
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext { toast(it) }
                            .toObservable()
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.FLOWABLE_LIST)
                .skipWhileInProgress(progressState)
                .flatMap {
                    getUsersUseCase.execute()
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext { toast(it.toString()) }
                            .toObservable()
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.SINGLE)
                .skipWhileInProgress(progressState)
                .flatMap {
                    getUserPhoneUseCase.execute(EXAMPLE_ID)
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { toast(it) }
                            .toObservable()
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.SINGLE_LIST)
                .skipWhileInProgress(progressState)
                .flatMap {
                    getSuggestedFriendsUseCase.execute(EXAMPLE_ID)
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { toast(it.toString()) }
                            .toObservable()
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)

        bus.observable()
                .filterByType(InteractorItem.Type.COMPLETABLE)
                .skipWhileInProgress(progressState)
                .flatMapCompletable {
                    resetPasswordUseCase.execute(EXAMPLE_ID)
                            .bindProgress(progressState)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete { toast("Request for password reset sent.") }
                }.retry()
                .subscribe()
                .addTo(compositeDisposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun prepareItems(): List<ListItem> {
        return arrayListOf(
                InteractorItem(InteractorItem.Type.OBSERVABLE, "GetUserPhotoUseCase"),
                InteractorItem(InteractorItem.Type.OBSERVABLE_LIST, "GetUserFriendsPhotoUseCase"),
                InteractorItem(InteractorItem.Type.FLOWABLE, "GetUserUseCase"),
                InteractorItem(InteractorItem.Type.FLOWABLE_LIST, "GetUsersUseCase"),
                InteractorItem(InteractorItem.Type.SINGLE, "GetUserPhoneUseCase"),
                InteractorItem(InteractorItem.Type.SINGLE_LIST, "GetSuggestedFriendsUseCase"),
                InteractorItem(InteractorItem.Type.COMPLETABLE, "ResetPasswordUseCase")
        )
    }

    private inline fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private inline fun <T> Observable<T>.skipWhileInProgress(progressState: Observable<Boolean>): Observable<T> {
        return this.withLatestFrom(progressState.startWith(false),
                BiFunction { t: T, inProgress: Boolean ->
                    Pair(t, inProgress)
                })
                .filter { !it.second }
                .map { it.first }
    }

    private inline fun Observable<Any>.filterByType(type: InteractorItem.Type): Observable<InteractorItem> {
        return this.filter { it is InteractorItem }
                .map { it as InteractorItem }
                .filter { it.type == type }
    }

    companion object {
        private const val EXAMPLE_ID = "EXAMPLE"
    }
}