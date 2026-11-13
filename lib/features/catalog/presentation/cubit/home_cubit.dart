import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';
import 'package:rang_shop/features/catalog/domain/repositories/product_repository.dart';

enum HomeStatus { loading, success, failure }

class HomeState {
  const HomeState({
    this.status = HomeStatus.loading,
    this.products = const [],
    this.errorMessage,
  });

  final HomeStatus status;
  final List<ProductEntity> products;
  final String? errorMessage;

  HomeState copyWith({
    HomeStatus? status,
    List<ProductEntity>? products,
    String? errorMessage,
  }) {
    return HomeState(
      status: status ?? this.status,
      products: products ?? this.products,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

class HomeCubit extends Cubit<HomeState> {
  HomeCubit(this._repository) : super(const HomeState());

  final ProductRepository _repository;

  Future<void> loadProducts() async {
    emit(state.copyWith(status: HomeStatus.loading));
    try {
      final products = await _repository.getProducts();
      if (isClosed) return;
      emit(state.copyWith(status: HomeStatus.success, products: products));
    } catch (e) {
      if (isClosed) return;
      emit(state.copyWith(
        status: HomeStatus.failure,
        errorMessage: e.toString(),
      ));
    }
  }

  void toggleFavorite(String productId) {
    final updated = state.products
        .map((p) => p.id == productId ? p.copyWith(isFavorite: !p.isFavorite) : p)
        .toList();
    emit(state.copyWith(products: updated));
  }
}
