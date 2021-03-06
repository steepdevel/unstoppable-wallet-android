package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendFeeInteractor(
        private val baseCurrency: Currency,
        private val rateManager: IRateManager,
        private val feeRateProvider: IFeeRateProvider?,
        private val coin: Coin)
    : SendFeeModule.IInteractor {

    var delegate: SendFeeModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    init {
        rateManager.marketInfoObservable(coin.code, baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { marketInfo ->
                    delegate?.didUpdateExchangeRate(marketInfo.rate)
                }
                .let {
                    disposables.add(it)
                }
    }

    override val feeRatePriorityList: List<FeeRatePriority> = feeRateProvider?.feeRatePriorityList ?: listOf()

    override val defaultFeeRatePriority: FeeRatePriority? = feeRateProvider?.defaultFeeRatePriority

    override fun getRate(coinCode: String): BigDecimal? {
        return rateManager.getLatestRate(coinCode, baseCurrency.code)
    }

    override fun syncFeeRate(feeRatePriority: FeeRatePriority) {
        if (feeRateProvider == null)
            return

        feeRateProvider.feeRate(feeRatePriority)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    delegate?.didUpdate(it)
                }, {
                    delegate?.didReceiveError(it as Exception)
                })
                .let { disposables.add(it) }
    }

    override fun onClear() {
        disposables.dispose()
    }
}
