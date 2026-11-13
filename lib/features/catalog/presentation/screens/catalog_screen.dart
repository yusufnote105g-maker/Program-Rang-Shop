import 'package:flutter/material.dart';
import 'package:rang_shop/core/localization/app_localizations.dart';

class CatalogScreen extends StatelessWidget {
  const CatalogScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final theme = Theme.of(context);

    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.search, size: 64, color: theme.colorScheme.primary),
          const SizedBox(height: 12),
          Text(
            t.translate('inDevelopment'),
            style: theme.textTheme.titleMedium,
          ),
        ],
      ),
    );
  }
}
