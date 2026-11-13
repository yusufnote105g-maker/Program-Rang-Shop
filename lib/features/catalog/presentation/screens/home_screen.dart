// lib/features/catalog/presentation/screens/home_screen.dart


import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/core/utils/price_formatter.dart';
import 'package:rang_shop/features/auth/domain/repositories/auth_repository.dart';
import 'package:rang_shop/features/auth/presentation/screens/auth_screen.dart';
import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';
import 'package:rang_shop/features/catalog/domain/repositories/product_repository.dart';
import 'package:rang_shop/features/catalog/presentation/bloc/home_cubit.dart';


class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.productRepository,
    required this.authRepository,
  });


  final ProductRepository productRepository;
  final AuthRepository authRepository;


  @override
  Widget build(BuildContext context) {
    return BlocProvider<HomeCubit>(
      create: (_) => HomeCubit(repository: productRepository)..load(),
      child: _HomeView(authRepository: authRepository),
    );
  }
}


class _HomeView extends StatelessWidget {
  const _HomeView({required this.authRepository});
  final AuthRepository authRepository;


  Future<void> _openAuth(BuildContext context) {
    return Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (BuildContext routeContext) => Material(
          type: MaterialType.transparency,
          child: Stack(
            children: [
              AuthScreen(
                repository: authRepository,
                onAuthSuccess: (_) => Navigator.of(routeContext).pop(),
              ),
              SafeArea(
                child: Padding(
                  padding: const EdgeInsets.all(4),
                  child: IconButton(
                    tooltip: 'Закрыть',
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(routeContext).pop(),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }


  void _onCartPressed(BuildContext context, UserEntity? user) {
    if (user == null) {
      _openAuth(context);
      return;
    }
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Экран корзины появится на следующем шаге'), behavior: SnackBarBehavior.floating));
  }


  void _onProfilePressed(BuildContext context, UserEntity? user) {
    if (user == null) {
      _openAuth(context);
      return;
    }
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (BuildContext sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ListTile(
                leading: const Icon(Icons.person_rounded),
                title: Text(_displayName(user)),
                subtitle: Text(_roleLabel(user.role)),
              ),
              const SizedBox(height: 8),
              FilledButton.tonal(
                onPressed: () {
                  authRepository.signOut();
                  Navigator.of(sheetContext).pop();
                },
                child: const Text('Выйти'),
              ),
            ],
          ),
        ),
      ),
    );
  }


  AppBar _buildAppBar(BuildContext context, UserEntity? user) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return AppBar(
      centerTitle: false,
      title: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(color: scheme.primary.withValues(alpha: 0.15), shape: BoxShape.circle),
            child: Icon(Icons.shopping_bag_rounded, color: scheme.primary, size: 22),
          ),
          const SizedBox(width: 10),
          const Text('Rang Shop', style: TextStyle(fontWeight: FontWeight.bold, letterSpacing: 0.5)),
        ],
      ),
      actions: [
        BlocSelector<HomeCubit, HomeState, int>(
          selector: (HomeState state) => state.cartCount,
          builder: (BuildContext context, int count) {
            return IconButton(
              tooltip: 'Корзина',
              onPressed: () => _onCartPressed(context, user),
              icon: Badge(
                isLabelVisible: count > 0,
                backgroundColor: scheme.primary,
                textColor: scheme.onPrimary,
                label: Text(count > 99 ? '99+' : '$count'),
                child: const Icon(Icons.shopping_cart_outlined),
              ),
            );
          },
        ),
        IconButton(
          tooltip: 'Профиль',
          onPressed: () => _onProfilePressed(context, user),
          icon: Icon(user == null ? Icons.person_outline_rounded : Icons.person_rounded),
        ),
        const SizedBox(width: 4),
      ],
    );
  }


  @override
  Widget build(BuildContext context) {
    return StreamBuilder<UserEntity?>(
      stream: authRepository.authStateChanges,
      initialData: authRepository.currentUser,
      builder: (BuildContext context, AsyncSnapshot<UserEntity?> snapshot) {
        final UserEntity? user = snapshot.data;
        return Scaffold(
          appBar: _buildAppBar(context, user),
          body: BlocBuilder<HomeCubit, HomeState>(
            builder: (BuildContext context, HomeState state) {
              return switch (state.status) {
                HomeStatus.initial || HomeStatus.loading => const Center(child: CircularProgressIndicator()),
                HomeStatus.failure => _ErrorView(message: state.errorMessage ?? 'Произошла ошибка', onRetry: context.read<HomeCubit>().load),
                HomeStatus.loaded => _CatalogView(state: state),
              };
            },
          ),
        );
      },
    );
  }
}


String _displayName(UserEntity user) {
  final String? name = user.fullName?.trim();
  if (name != null && name.isNotEmpty) return name;
  return user.phone ?? user.email ?? 'Профиль';
}


String _roleLabel(UserRole role) {
  return switch (role) {
    UserRole.customer => 'Покупатель',
    UserRole.owner => 'Продавец',
    UserRole.admin => 'Администратор',
  };
}


class _CatalogView extends StatelessWidget {
  const _CatalogView({required this.state});
  final HomeState state;


  @override
  Widget build(BuildContext context) {
    final HomeCubit cubit = context.read<HomeCubit>();
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final List<String> categories = state.categories;
    final List<ProductEntity> products = state.visibleProducts;


    return Column(
      children: [
        SizedBox(
          height: 56,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            itemCount: categories.length,
            separatorBuilder: (_, __) => const SizedBox(width: 8),
            itemBuilder: (BuildContext context, int index) {
              final String category = categories[index];
              final bool isSelected = category == state.selectedCategory;
              return ChoiceChip(
                label: Text(category),
                selected: isSelected,
                showCheckmark: false,
                side: BorderSide.none,
                shape: const StadiumBorder(),
                backgroundColor: scheme.surfaceContainerHighest,
                selectedColor: scheme.primary,
                labelStyle: TextStyle(color: isSelected ? scheme.onPrimary : scheme.onSurface, fontWeight: isSelected ? FontWeight.bold : FontWeight.w500),
                onSelected: (_) => cubit.selectCategory(category),
              );
            },
          ),
        ),
        Expanded(
          child: products.isEmpty
              ? const Center(child: Text('В этой категории пока нет товаров'))
              : GridView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(maxCrossAxisExtent: 220, mainAxisExtent: 300, mainAxisSpacing: 12, crossAxisSpacing: 12),
                  itemCount: products.length,
                  itemBuilder: (BuildContext context, int index) {
                    final ProductEntity product = products[index];
                    return _ProductCard(
                      product: product,
                      onAddToCart: () {
                        cubit.addToCart(product.id);
                        ScaffoldMessenger.of(context)
                          ..hideCurrentSnackBar()
                          ..showSnackBar(SnackBar(content: Text('«${product.title}» добавлен в корзину'), behavior: SnackBarBehavior.floating, duration: const Duration(milliseconds: 1500)));
                      },
                    );
                  },
                ),
        ),
      ],
    );
  }
}


class _ProductCard extends StatelessWidget {
  const _ProductCard({required this.product, required this.onAddToCart});
  final ProductEntity product;
  final VoidCallback onAddToCart;


  static const double _titleFontSize = 14;
  static const double _titleLineHeight = 1.3;


  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final double titleHeight = MediaQuery.textScalerOf(context).scale(_titleFontSize) * _titleLineHeight * 2;


    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      color: scheme.surfaceContainerHighest,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: CachedNetworkImage(
              imageUrl: product.imageUrl,
              width: double.infinity,
              fit: BoxFit.cover,
              memCacheWidth: 600,
              placeholder: (BuildContext context, String url) => ColoredBox(
                color: scheme.onSurface.withValues(alpha: 0.06),
                child: const Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))),
              ),
              errorWidget: (BuildContext context, String url, Object error) => ColoredBox(
                color: scheme.onSurface.withValues(alpha: 0.06),
                child: Icon(Icons.image_not_supported_outlined, color: scheme.onSurface.withValues(alpha: 0.4)),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  height: titleHeight,
                  child: Text(product.title, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: _titleFontSize, height: _titleLineHeight, fontWeight: FontWeight.w500)),
                ),
                const SizedBox(height: 4),
                Text(PriceFormatter.formatFromDirams(product.priceDirams), style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: scheme.primary)),
                const SizedBox(height: 8),
                FilledButton(
                  onPressed: onAddToCart,
                  style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(40), padding: EdgeInsets.zero, shape: const StadiumBorder()),
                  child: const Text('В корзину', style: TextStyle(fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}


class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;


  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded, size: 48),
            const SizedBox(height: 16),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton(onPressed: onRetry, child: const Text('Повторить')),
          ],
        ),
      ),
    );
  }
}
