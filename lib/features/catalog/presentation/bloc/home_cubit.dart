// lib/features/catalog/presentation/bloc/home_cubit.dart


import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';
import 'package:rang_shop/features/catalog/domain/repositories/product_repository.dart';


enum HomeStatus { initial, loading, loaded, failure }


class HomeState extends Equatable {
  const HomeState({
    this.status = HomeStatus.initial,
    this.products = const <ProductEntity>[],
    this.selectedCategory = allCategories,
    this.cart = const <String, int>{},
    this.errorMessage,
  });


  static const String allCategories = 'Все';


  final HomeStatus status;
  final List<ProductEntity> products;
  final String selectedCategory;
  final Map<String, int> cart;
  final String? errorMessage;


  List<String> get categories {
    final Set<String> seen = <String>{};
    for (final ProductEntity product in products) {
      seen.add(product.category);
    }
    return <String>[allCategories, ...seen];
  }


  List<ProductEntity> get visibleProducts {
    if (selectedCategory == allCategories) return products;
    return products.where((ProductEntity p) => p.category == selectedCategory).toList(growable: false);
  }


  int get cartCount => cart.values.fold<int>(0, (int sum, int qty) => sum + qty);


  HomeState copyWith({
    HomeStatus? status,
    List<ProductEntity>? products,
    String? selectedCategory,
    Map<String, int>? cart,
    String? errorMessage,
    bool clearError = false,
  }) {
    return HomeState(
      status: status ?? this.status,
      products: products ?? this.products,
      selectedCategory: selectedCategory ?? this.selectedCategory,
      cart: cart ?? this.cart,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }


  @override
  List<Object?> get props => [status, products, selectedCategory, cart, errorMessage];
}


class HomeCubit extends Cubit<HomeState> {
  HomeCubit({required ProductRepository repository})
      : _repository = repository,
        super(const HomeState());


  final ProductRepository _repository;


  Future<void> load() async {
    if (state.status == HomeStatus.loading) return;
    emit(state.copyWith(status: HomeStatus.loading, clearError: true));
    try {
      final List<ProductEntity> products = await _repository.getProducts();
      if (isClosed) return;
      emit(state.copyWith(status: HomeStatus.loaded, products: products, selectedCategory: HomeState.allCategories));
    } catch (_) {
      if (isClosed) return;
      emit(state.copyWith(status: HomeStatus.failure, errorMessage: 'Не удалось загрузить товары. Проверьте интернет и попробуйте снова'));
    }
  }


  void selectCategory(String category) {
    if (state.selectedCategory == category) return;
    emit(state.copyWith(selectedCategory: category));
  }


  void addToCart(String productId) {
    final Map<String, int> updated = Map<String, int>.of(state.cart);
    updated[productId] = (updated[productId] ?? 0) + 1;
    emit(state.copyWith(cart: updated));
  }
}
